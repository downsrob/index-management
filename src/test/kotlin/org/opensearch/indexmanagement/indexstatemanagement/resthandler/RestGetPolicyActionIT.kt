/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.indexmanagement.indexstatemanagement.resthandler

import org.opensearch.indexmanagement.IndexManagementPlugin.Companion.POLICY_BASE_URI
import org.opensearch.indexmanagement.indexstatemanagement.IndexStateManagementRestTestCase
import org.opensearch.indexmanagement.indexstatemanagement.model.Policy
import org.opensearch.indexmanagement.makeRequest
import org.opensearch.indexmanagement.opensearchapi.convertToMap
import org.opensearch.rest.RestStatus

@Suppress("UNCHECKED_CAST")
class RestGetPolicyActionIT : IndexStateManagementRestTestCase() {

    fun `test get single policy`() {
        val policy = createRandomPolicy()

        val response = client().makeRequest(
            "GET",
            "$POLICY_BASE_URI/${policy.id}"
        )
        assertEquals("Unexpected RestStatus", RestStatus.OK, response.restStatus())
        val actualPolicyResponse = response.asMap()["policy"]
        val expectedPolicyResponse = policy.convertToMap()["policy"] as MutableMap<String, Any>
        expectedPolicyResponse.remove("user")
        assertEquals("Get policy response did not match the actual policy", expectedPolicyResponse, actualPolicyResponse)
    }

    fun `test get all policies`() {
        val policies: MutableList<Policy> = randomList(1, 10) { createRandomPolicy() }

        val response = client().makeRequest(
            "GET",
            "$POLICY_BASE_URI/"
        )
        assertEquals("Unexpected RestStatus", RestStatus.OK, response.restStatus())
        val responseMap = response.asMap()
        assertEquals("Get policy response did not include all policies", policies.size, responseMap["total_policies"])
        val responsePolicies = responseMap["policies"] as ArrayList<MutableMap<String, Any>>
        policies.forEach { policy ->
            val expectedPolicyResponse = policy.convertToMap()["policy"] as MutableMap<String, Any>
            expectedPolicyResponse.remove("user")
            val actualPolicyResponseMap = responsePolicies.find { (it["policy"] as MutableMap<String, Any>)["policy_id"] == policy.id }
                as MutableMap<String, Any>
            val actualPolicyResponse = actualPolicyResponseMap["policy"]
            assertEquals("Get policy response did not match the actual policies", expectedPolicyResponse, actualPolicyResponse)
        }
    }
}
