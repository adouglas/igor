/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.netflix.spinnaker.igor.jenkins.service

import com.netflix.spinnaker.hystrix.SimpleHystrixCommand
import com.netflix.spinnaker.igor.jenkins.client.JenkinsClient
import com.netflix.spinnaker.igor.jenkins.client.model.Build
import com.netflix.spinnaker.igor.jenkins.client.model.BuildDependencies
import com.netflix.spinnaker.igor.jenkins.client.model.BuildsList
import com.netflix.spinnaker.igor.jenkins.client.model.JobConfig
import com.netflix.spinnaker.igor.jenkins.client.model.JobList
import com.netflix.spinnaker.igor.jenkins.client.model.Project
import com.netflix.spinnaker.igor.jenkins.client.model.ProjectsList
import com.netflix.spinnaker.igor.jenkins.client.model.QueuedJob
import com.netflix.spinnaker.igor.jenkins.client.model.ScmDetails
import org.springframework.web.util.UriUtils
import retrofit.client.Response

class JenkinsService {
    final String groupKey
    final JenkinsClient jenkinsClient

    JenkinsService(String jenkinsHostId, JenkinsClient jenkinsClient) {
        this.groupKey = "jenkins-${jenkinsHostId}"
        this.jenkinsClient = jenkinsClient
    }

    private String encode(uri) {
        return UriUtils.encodeFragment(uri, "UTF-8")
    }

    ProjectsList getProjects() {
        new SimpleHystrixCommand<ProjectsList>(
            groupKey, "getProjects", {
            List<Project> projects = []
            def recursiveGetProjects
            recursiveGetProjects = { list, prefix="" ->
                if (prefix) {
                    prefix = prefix + "/job/"
                }
                list.each {
                    if (it.list == null || it.list.empty) {
                        it.name = prefix + it.name
                        projects << it
                    } else {
                        recursiveGetProjects(it.list, prefix + it.name)
                    }
                }
            }
            recursiveGetProjects(jenkinsClient.getProjects().list)
            ProjectsList projectList = new ProjectsList()
            projectList.list = projects
            return projectList
        }).execute()
    }

    JobList getJobs() {
        new SimpleHystrixCommand<JobList>(
            groupKey, "getJobs", {
            return jenkinsClient.getJobs()
        }).execute()
    }

    BuildsList getBuilds(String jobName) {
        new SimpleHystrixCommand<BuildsList>(
            groupKey, "getBuilds", {
            return jenkinsClient.getBuilds(encode(jobName))
        }).execute()
    }

    BuildDependencies getDependencies(String jobName) {
        new SimpleHystrixCommand<BuildDependencies>(
            groupKey, "getDependencies", {
            return jenkinsClient.getDependencies(encode(jobName))
        }).execute()
    }

    Build getBuild(String jobName, Integer buildNumber) {
        return jenkinsClient.getBuild(encode(jobName), buildNumber)
    }

    ScmDetails getGitDetails(String jobName, Integer buildNumber) {
        new SimpleHystrixCommand<ScmDetails>(
            groupKey, "getGitDetails", {
            return jenkinsClient.getGitDetails(encode(jobName), buildNumber)
        }).execute()
    }

    Build getLatestBuild(String jobName) {
        new SimpleHystrixCommand<Build>(
            groupKey, "getLatestBuild", {
            return jenkinsClient.getLatestBuild(encode(jobName))
        }).execute()
    }

    QueuedJob getQueuedItem(Integer item) {
        return jenkinsClient.getQueuedItem(item)
    }

    Response build(String jobName) {
        return jenkinsClient.build(encode(jobName))
    }

    Response buildWithParameters(String jobName, Map<String, String> queryParams) {
        return jenkinsClient.buildWithParameters(encode(jobName), queryParams)
    }

    JobConfig getJobConfig(String jobName) {
        return jenkinsClient.getJobConfig(encode(jobName))
    }

    Response getPropertyFile(String jobName, Integer buildNumber, String fileName) {
        return jenkinsClient.getPropertyFile(encode(jobName), buildNumber, fileName)
    }
}