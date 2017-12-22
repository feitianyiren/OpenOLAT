/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.core.gui.components.form.flexible.impl.elements.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The row must implement hashCode() and equals() methods to use the
 * functionalities of the class.
 * 
 * 
 * Initial date: 15 nov. 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 * @param <U>
 */
public abstract class DefaultFlexiTreeTableDataModel<U extends FlexiTreeTableNode> extends DefaultFlexiTableDataModel<U>
implements FlexiTreeTableDataModel<U>, FilterableFlexiTableModel {
	
	protected final Set<U> openedRows = new HashSet<>();
	protected List<U> backupRows;
	protected U focusedNode;

	public DefaultFlexiTreeTableDataModel(FlexiTableColumnModel columnModel) {
		super(columnModel);
	}

	@Override
	public int getIndentation(int row) {
		FlexiTreeTableNode node = getObject(row);
		return getIndentation(node);
	}
	
	private final int getIndentation(FlexiTreeTableNode node) {
		int indentation = 0;
		for(FlexiTreeTableNode parent=node.getParent(); parent != null; parent=parent.getParent()) {
			indentation++;
		}
		return indentation;
	}
	
	protected final void setFilteredObjects(List<U> objects) {
		super.setObjects(objects);
	}
	
	protected final void setUnfilteredObjects() {
		List<U> rows;
		if(focusedNode != null) {
			int row = backupRows.indexOf(focusedNode);
			rows = focusedNodes(backupRows, focusedNode, row);
		} else {
			rows = new ArrayList<>(backupRows);
		}
		
		List<U> opened = new ArrayList<>();
		for(U row:rows) {
			if(row.getParent() == null || openedRows.contains(row.getParent())) {
				opened.add(row);
			}
		}
		super.setObjects(opened);
	}

	@Override
	public final void setObjects(List<U> objects) {
		backupRows = objects;
		super.setObjects(objects);
	}
	
	@Override
	public boolean hasChildren(int row) {
		return true;
	}

	@Override
	public boolean isOpen(int row) {
		U object = getObject(row);
		U nextObject = getObject(row + 1);
		if(nextObject == null || !object.equals(nextObject.getParent())) {
			return false;
		}
		return true;
	}

	@Override
	public void focus(int row) {
		U object = getObject(row);
		List<U> currentRows = getObjects();
		List<U> focusedRows = focusedNodes(currentRows, object, row);
		super.setObjects(focusedRows);
		focusedNode = object;
	}
	
	private List<U> focusedNodes(List<U> currentRows, U object, int row) {
		int depth = getIndentation(object);
		FlexiTreeTableNode parentObject = object.getParent();
		
		List<U> focusedRows = new ArrayList<>();
		focusedRows.add(object);
		for(int i=row + 1; i<currentRows.size(); i++) {
			U currentRow = currentRows.get(i);
			if((parentObject == null && currentRow.getParent() == null)
				|| (currentRow.getParent() == null)
				|| (parentObject != null && parentObject.equals(currentRow.getParent()))) {
				break;
			} else if(depth >= getIndentation(currentRow)) {
				break;
			}
			focusedRows.add(currentRow);
		}
		return focusedRows;
	}

	@Override
	public void popBreadcrumb(FlexiTreeTableNode node) {
		int row = backupRows.indexOf(node);
		if(row < 0) {
			super.setObjects(backupRows);
			focusedNode = null;
		} else {
			FlexiTreeTableNode parentObject = node.getParent();
			List<U> focusedRows = new ArrayList<>();
			for(int i=row; i<backupRows.size(); i++) {
				U currentRow = backupRows.get(i);
				if(!node.equals(currentRow)
					&& ((parentObject == null && currentRow.getParent() == null)
							|| (parentObject != null && parentObject.equals(currentRow.getParent())))) {
					break;
				}
				focusedRows.add(currentRow);
			}
			super.setObjects(focusedRows);
			focusedNode = (U)node;
		}
	}

	@Override
	public void open(int row) {
		U objectToOpen = getObject(row);
		openedRows.add(objectToOpen);
		
		List<U> currentRows = getObjects();//this is already a copy
		
		List<U> children = new ArrayList<>();
		for(U backupRow:backupRows) {
			if(objectToOpen.equals(backupRow.getParent())) {
				children.add(backupRow);
			}
		}
		
		int childrenPos = row + 1;
		if(childrenPos < currentRows.size()) {
			currentRows.addAll(childrenPos, children);
		} else {
			currentRows.addAll(children);
		}
		super.setObjects(currentRows);
	}

	@Override
	public void close(int row) {
		U objectToClose = getObject(row);

		boolean start = false;
		FlexiTreeTableNode parentOf = objectToClose.getParent();
		
		List<U> currentRows = getObjects();
		List<U> closedRows = new ArrayList<>();
		for(U currentRow:currentRows) {
			if(start) {
				if((parentOf == null && currentRow.getParent() == null)
					|| (currentRow.getParent() == null)
					|| (parentOf != null && parentOf.equals(currentRow.getParent()))) {
					start = false;
				} else {
					openedRows.remove(currentRow);
					continue;
				}
			} else if(currentRow.equals(objectToClose)) {
				openedRows.remove(objectToClose);
				start = true;
			}
			closedRows.add(currentRow);
		}
		super.setObjects(closedRows);
	}
}
