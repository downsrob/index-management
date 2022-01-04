/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.indexmanagement.indexstatemanagement.resthandler

import org.opensearch.client.ResponseException
import org.opensearch.indexmanagement.IndexManagementPlugin.Companion.POLICY_BASE_URI
import org.opensearch.indexmanagement.indexstatemanagement.IndexStateManagementRestTestCase
import org.opensearch.indexmanagement.makeRequest
import org.opensearch.indexmanagement.waitFor
import org.opensearch.rest.RestStatus
import kotlin.test.assertFailsWith

@Suppress("UNCHECKED_CAST")
class RestDeletePolicyActionIT : IndexStateManagementRestTestCase() {
    fun `test delete single policy`() {
        // Create a policy and verify that it exists
        val policy = createRandomPolicy()
        waitFor {
            val responsePolicy = getPolicy(policy.id)
            assertEquals("Policy was not created", policy.id, responsePolicy.id)
        }

        val deleteResponse = client().makeRequest(
            "DELETE",
            "$POLICY_BASE_URI/${policy.id}"
        )
        assertEquals("Unexpected RestStatus", RestStatus.OK, deleteResponse.restStatus())

        // Verify that the policy is removed
        waitFor {
            assertFailsWith(ResponseException::class, "Expected ResponseException for deleted policy") {
                getPolicy(policy.id)
            }
        }
    }
}
