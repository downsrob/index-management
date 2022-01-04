/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.indexmanagement.spi.indexstatemanagement

import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.common.xcontent.ToXContent
import org.opensearch.common.xcontent.ToXContentObject
import org.opensearch.common.xcontent.XContentBuilder
import org.opensearch.indexmanagement.spi.indexstatemanagement.model.ActionMetaData
import org.opensearch.indexmanagement.spi.indexstatemanagement.model.ActionRetry
import org.opensearch.indexmanagement.spi.indexstatemanagement.model.ActionTimeout
import org.opensearch.indexmanagement.spi.indexstatemanagement.model.ManagedIndexMetaData
import org.opensearch.indexmanagement.spi.indexstatemanagement.model.StepContext
import java.time.Instant

abstract class Action(
    val type: String,
    val actionIndex: Int
) : ToXContentObject, Writeable {

    var configTimeout: ActionTimeout? = null
    var configRetry: ActionRetry? = ActionRetry(DEFAULT_RETRIES)
    var customAction: Boolean = false

    final override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        builder.startObject()
        configTimeout?.toXContent(builder, params)
        configRetry?.toXContent(builder, params)
        // Include a "custom" object wrapper for custom actions to allow extensions to put arbitrary action configs in the config
        // index. The EXCLUDE_CUSTOM_FIELD_PARAM is used to not include this wrapper in api responses
        if (customAction && !params.paramAsBoolean(EXCLUDE_CUSTOM_FIELD_PARAM, false)) builder.startObject(CUSTOM_ACTION_FIELD)
        populateAction(builder, params)
        if (customAction && !params.paramAsBoolean(EXCLUDE_CUSTOM_FIELD_PARAM, false)) builder.endObject()
        return builder.endObject()
    }

    /**
     * The implementer of Action can change this method to correctly serialize the internals of the action
     * when stored internally or returned as response
     */
    open fun populateAction(builder: XContentBuilder, params: ToXContent.Params) {
        builder.startObject(type).endObject()
    }

    final override fun writeTo(out: StreamOutput) {
        out.writeString(type)
        out.writeOptionalWriteable(configTimeout)
        out.writeOptionalWriteable(configRetry)
        populateAction(out)
    }

    fun getUpdatedActionMetadata(managedIndexMetaData: ManagedIndexMetaData, stateName: String): ActionMetaData {
        val stateMetaData = managedIndexMetaData.stateMetaData
        val actionMetaData = managedIndexMetaData.actionMetaData

        return when {
            // start a new action
            stateMetaData?.name != stateName ->
                ActionMetaData(this.type, Instant.now().toEpochMilli(), this.actionIndex, false, 0, 0, null)
            actionMetaData?.index != this.actionIndex ->
                ActionMetaData(this.type, Instant.now().toEpochMilli(), this.actionIndex, false, 0, 0, null)
            // RetryAPI will reset startTime to null for actionMetaData and we'll reset it to "now" here
            else -> actionMetaData.copy(startTime = actionMetaData.startTime ?: Instant.now().toEpochMilli())
        }
    }

    /**
     * The implementer of Action can change this method to correctly serialize the internals of the action
     * when data is shared between nodes
     */
    open fun populateAction(out: StreamOutput) {
        out.writeInt(actionIndex)
    }

    /**
     * Get all the steps associated with the action
     */
    abstract fun getSteps(): List<Step>

    /**
     * Get the current step to execute in the action
     */
    abstract fun getStepToExecute(context: StepContext): Step

    final fun isLastStep(stepName: String): Boolean = getSteps().last().name == stepName

    final fun isFirstStep(stepName: String): Boolean = getSteps().first().name == stepName

    companion object {
        const val DEFAULT_RETRIES = 3L
        const val CUSTOM_ACTION_FIELD = "custom"
        const val EXCLUDE_CUSTOM_FIELD_PARAM = "exclude_custom"
    }
}
