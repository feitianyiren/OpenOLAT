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
package org.olat.course.nodes.st.assessment;

import org.olat.core.util.StringHelper;
import org.olat.course.assessment.handler.AssessmentConfig;
import org.olat.course.nodes.STCourseNode;
import org.olat.course.run.scoring.ScoreCalculator;
import org.olat.modules.ModuleConfiguration;

/**
 * 
 * Initial date: 19 Aug 2019<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class STAssessmentConfig implements AssessmentConfig {
	
	private final boolean isRoot;
	private final ModuleConfiguration rootConfig;
	private final ScoreCalculator scoreCalculator;

	public STAssessmentConfig(boolean isRoot, ModuleConfiguration rootConfig, ScoreCalculator scoreCalculator) {
		this.isRoot = isRoot;
		this.rootConfig = rootConfig;
		this.scoreCalculator = scoreCalculator;
	}

	@Override
	public boolean isAssessable() {
		return true;
	}

	@Override
	public boolean ignoreInCourseAssessment() {
		return false;
	}

	@Override
	public void setIgnoreInCourseAssessment(boolean ignoreInCourseAssessment) {
		//
	}

	@Override
	public Mode getScoreMode() {
		if (scoreCalculator != null && StringHelper.containsNonWhitespace(scoreCalculator.getScoreExpression())) {
			return Mode.evaluated;
		} else if (rootConfig.has(STCourseNode.CONFIG_SCORE_KEY)) {
			return Mode.evaluated;
		}
		return Mode.none;
	}

	@Override
	public Float getMaxScore() {
		return null;
	}

	@Override
	public Float getMinScore() {
		return null;
	}

	@Override
	public Mode getPassedMode() {
		if (scoreCalculator != null && StringHelper.containsNonWhitespace(scoreCalculator.getPassedExpression())) {
			return Mode.evaluated;
		} else if (isEvaluatedRoot()) {
			return Mode.evaluated;
		} else if (isRoot && rootConfig.getBooleanSafe(STCourseNode.CONFIG_PASSED_MANUALLY)) {
			return Mode.setByNode;
		}
		return Mode.none;
	}

	private boolean isEvaluatedRoot() {
		return isRoot && (
				   rootConfig.has(STCourseNode.CONFIG_PASSED_PROGRESS)
				|| rootConfig.has(STCourseNode.CONFIG_PASSED_ALL)
				|| rootConfig.has(STCourseNode.CONFIG_PASSED_POINTS)
				);
	}
	
	@Override
	public Float getCutValue() {
		if (scoreCalculator != null && ScoreCalculator.PASSED_TYPE_CUTVALUE.equals(scoreCalculator.getPassedType())) {
			return Float.valueOf(scoreCalculator.getPassedCutValue());
		}
		return null;
	}

	@Override
	public Mode getCompletionMode() {
		return Mode.evaluated;
	}

	@Override
	public boolean hasAttempts() {
		return false;
	}

	@Override
	public boolean hasComment() {
		return false;
	}

	@Override
	public boolean hasIndividualAsssessmentDocuments() {
		return false;
	}

	@Override
	public boolean hasStatus() {
		return false;
	}

	@Override
	public boolean isAssessedBusinessGroups() {
		return false;
	}

	@Override
	public boolean isEditable() {
		// ST nodes never editable, data generated on the fly
		return false;
	}

	@Override
	public boolean isBulkEditable() {
		return false;
	}

	@Override
	public boolean hasEditableDetails() {
		return false;
	}
	
	@Override
	public boolean isExternalGrading() {
		return false;
	}

	@Override
	public boolean isObligationOverridable() {
		return false;
	}
}
