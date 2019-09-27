/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.search.service.indexer.repository;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.olat.core.CoreSpringFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.StartupException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.nodes.INode;
import org.olat.course.CorruptedCourseException;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.nodeaccess.NodeAccessService;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.navigation.NavigationHandler;
import org.olat.course.run.userview.CourseTreeNode;
import org.olat.course.run.userview.TreeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;
import org.olat.course.run.userview.VisibleTreeFilter;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryStatusEnum;
import org.olat.repository.RepositoryManager;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.indexer.AbstractHierarchicalIndexer;
import org.olat.search.service.indexer.Indexer;
import org.olat.search.service.indexer.OlatFullIndexer;
import org.olat.search.service.indexer.repository.course.CourseNodeEntry;
import org.olat.search.service.indexer.repository.course.CourseNodeIndexer;

/**
 * Index a whole course.
 * @author Christian Guretzki
 */
public class CourseIndexer extends AbstractHierarchicalIndexer {
	private static final Logger log = Tracing.createLoggerFor(CourseIndexer.class);
	public static final String TYPE = "type.repository.entry.CourseModule"; 
	
	private RepositoryManager repositoryManager;
	
	/**
	 * [used by Spring]
	 * @param repositoryManager
	 */
	public void setRepositoryManager(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}

	@Override
	public String getSupportedTypeName() {	
		return CourseModule.getCourseTypeName(); 
	}
	
	@Override
	public void doIndex(SearchResourceContext parentResourceContext, Object parentObject, OlatFullIndexer indexWriter) {
		RepositoryEntry repositoryEntry = (RepositoryEntry) parentObject;
		if (log.isDebugEnabled()) log.debug("Analyse Course... repositoryEntry=" + repositoryEntry);
		try {
			RepositoryEntryStatusEnum status = repositoryEntry.getEntryStatus();
			if(status.decommissioned()) {
				if(log.isDebugEnabled()) log.debug("Course not indexed because it's " + status + ": repositoryEntry=" + repositoryEntry);
				return;
			}

			ICourse course = CourseFactory.loadCourse(repositoryEntry);
			// course.getCourseTitle(); // do not index title => index root-node
			parentResourceContext.setParentContextType(TYPE);
			parentResourceContext.setParentContextName(course.getCourseTitle());
			doIndexCourse( parentResourceContext, course,  course.getRunStructure().getRootNode(), indexWriter);			
		} catch(CorruptedCourseException ex) {
			log.warn("Can not index repositoryEntry (" + repositoryEntry.getKey() + ")", ex);
		} catch (Exception ex) {
			log.warn("Can not index repositoryEntry=" + repositoryEntry,ex);
		}
	}

	/**
	 * 
	 * @param repositoryResourceContext
	 * @param course
	 * @param courseNode
	 * @param indexWriter
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void doIndexCourse(SearchResourceContext repositoryResourceContext, ICourse course, INode node, OlatFullIndexer indexWriter)
	throws IOException,InterruptedException  {
		//try to index the course node
		if(node instanceof CourseNode) {
			if (log.isDebugEnabled()) log.debug("Analyse CourseNode child ... childCourseNode=" + node);
			// go further with resource
			CourseNode childCourseNode = (CourseNode)node;
			CourseNodeIndexer courseNodeIndexer = getCourseNodeIndexer(childCourseNode);
			if (courseNodeIndexer != null) {
				if (log.isDebugEnabled()) {
					log.debug("courseNodeIndexer=" + courseNodeIndexer);
				}
				
 				try {
					courseNodeIndexer.doIndex(repositoryResourceContext, course, childCourseNode, indexWriter);
				} catch (Exception e) {
					log.warn("Can not index course node=" + childCourseNode.getIdent(), e);
				}
			}
		}
		
		//loop over all child nodes
		int childCount = node.getChildCount();
		for (int i=0;i<childCount; i++) {
			INode childNode = node.getChildAt(i);
			doIndexCourse(repositoryResourceContext, course, childNode, indexWriter);
		}
	}

	/**
	 * Bean setter method used by spring. 
	 * @param indexerList
	 */
	@Override
	public void setIndexerList(List<Indexer> indexerList) {
		for (Indexer courseNodeIndexer : indexerList) {
			if(!(courseNodeIndexer instanceof CourseNodeIndexer)) {
				throw new StartupException("Configured indexer is not of type RepositoryEntryIndexer: " + courseNodeIndexer);
			}
		}
		super.setIndexerList(indexerList);
	}

	@Override
	public boolean checkAccess(ContextEntry contextEntry, BusinessControl businessControl, Identity identity, Roles roles) {
		ContextEntry bcContextEntry = businessControl.popLauncherContextEntry();
		if (bcContextEntry == null) {
			// no context-entry anymore, the repository entry itself is the context entry, 
			// not a course node of course we have access to the course metadata
			return true;
		}
		if (log.isDebugEnabled()) log.debug("Start identity=" + identity + "  roles=" + roles);
		Long repositoryKey = contextEntry.getOLATResourceable().getResourceableId();
		RepositoryEntry repositoryEntry = repositoryManager.lookupRepositoryEntry(repositoryKey);
		if (log.isDebugEnabled()) log.debug("repositoryEntry=" + repositoryEntry );

		if(roles.isGuestOnly() && repositoryEntry.isGuests()) {
			return false;
		}
		
		Long nodeId = bcContextEntry.getOLATResourceable().getResourceableId();
		if (log.isDebugEnabled()) log.debug("nodeId=" + nodeId );
		ICourse course = CourseFactory.loadCourse(repositoryEntry);
		
		IdentityEnvironment ienv = new IdentityEnvironment();
		ienv.setIdentity(identity);
		ienv.setRoles(roles);
		UserCourseEnvironment userCourseEnv = new UserCourseEnvironmentImpl(ienv, course.getCourseEnvironment());
		if (log.isDebugEnabled()) log.debug("userCourseEnv=" + userCourseEnv + "ienv=" + ienv );
		
		String nodeIdS = nodeId.toString();
		CourseNode courseNode = course.getRunStructure().getNode(nodeIdS);
		if (log.isDebugEnabled()) log.debug("courseNode=" + courseNode );
		
		TreeEvaluation treeEval = new TreeEvaluation();
		NodeAccessService nodeAccessService = CoreSpringFactory.getImpl(NodeAccessService.class);
		CourseTreeNode courseTreeNode = (CourseTreeNode)nodeAccessService.getCourseTreeModelBuilder(userCourseEnv)
				.build(treeEval, new VisibleTreeFilter())
				.getRootNode();

		CourseTreeNode newCalledTreeNode = treeEval.getCorrespondingTreeNode(courseNode);
		if (newCalledTreeNode == null) {
			// TreeNode no longer visible
			return false;
		}

		if (!newCalledTreeNode.isVisible()) throw new AssertException("node eval not visible!!");
		if (log.isDebugEnabled()) log.debug("call mayAccessWholeTreeUp..." );
		boolean mayAccessWholeTreeUp = NavigationHandler.mayAccessWholeTreeUp(courseTreeNode);
		if (log.isDebugEnabled()) log.debug("call mayAccessWholeTreeUp=" + mayAccessWholeTreeUp );
		
		if (mayAccessWholeTreeUp) {
			CourseNodeIndexer courseNodeIndexer = getCourseNodeIndexer(courseNode);
			bcContextEntry.setTransientState(new CourseNodeEntry(courseNode));
			return courseNodeIndexer.checkAccess(bcContextEntry, businessControl, identity, roles)
					&& super.checkAccess(bcContextEntry, businessControl, identity, roles);		
		} else {
			return false;
		}
	}
	
	private CourseNodeIndexer getCourseNodeIndexer(CourseNode node) {
		String courseNodeName = node.getClass().getName();
		List<Indexer> courseNodeIndexer = getIndexerByType(courseNodeName);
		if (courseNodeIndexer != null && !courseNodeIndexer.isEmpty()) {
			return (CourseNodeIndexer)courseNodeIndexer.get(0);
		}
		return null;
	}
}
