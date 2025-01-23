/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.wgzhao.addax.core.container.util;

import com.wgzhao.addax.common.util.Configuration;
import com.wgzhao.addax.core.util.container.CoreConstant;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.wgzhao.addax.common.base.Constant.LOAD_BALANCE_RESOURCE_MARK;
import static com.wgzhao.addax.core.util.container.CoreConstant.CORE_CONTAINER_TASK_GROUP_CHANNEL;
import static com.wgzhao.addax.core.util.container.CoreConstant.CORE_CONTAINER_TASK_GROUP_ID;
import static com.wgzhao.addax.core.util.container.CoreConstant.JOB_CONTENT;
import static com.wgzhao.addax.core.util.container.CoreConstant.JOB_READER_PARAMETER;
import static com.wgzhao.addax.core.util.container.CoreConstant.JOB_WRITER_PARAMETER;

public final class JobAssignUtil {
    private JobAssignUtil() {
    }

    /**
     * 公平的分配 task 到对应的 taskGroup 中。
     * 公平体现在：会考虑 task 中对资源负载作的 load 标识进行更均衡的作业分配操作。
     *
     * @param configuration        configuration
     * @param channelNumber        the number of channel
     * @param channelsPerTaskGroup the channel of task group
     * @return list of configuration
     */
    public static List<Configuration> assignFairly(Configuration configuration, int channelNumber, int channelsPerTaskGroup) {
        Validate.isTrue(configuration != null, "The `job.content` can not be null.");

        List<Configuration> contentConfig = configuration.getListConfiguration(JOB_CONTENT);
        Validate.isTrue(!contentConfig.isEmpty(), "The `job.content` is empty");

        Validate.isTrue(channelNumber > 0 && channelsPerTaskGroup > 0,
                "The average number of tasks per channel [averTaskPerChannel], the number of channels [channelNumber], and the average number of channels per task group " +
                        "[channelsPerTaskGroup] should all be positive numbers.");

        int taskGroupNumber = (int) Math.ceil(1.0 * channelNumber / channelsPerTaskGroup);

        Configuration aTaskConfig = contentConfig.get(0);

        String readerResourceMark = aTaskConfig.getString(JOB_READER_PARAMETER + "." +
                LOAD_BALANCE_RESOURCE_MARK);
        String writerResourceMark = aTaskConfig.getString(JOB_WRITER_PARAMETER + "." +
                LOAD_BALANCE_RESOURCE_MARK);

        boolean hasLoadBalanceResourceMark = StringUtils.isNotBlank(readerResourceMark) ||
                StringUtils.isNotBlank(writerResourceMark);

        if (!hasLoadBalanceResourceMark) {
            // fake 一个固定的 key 作为资源标识（在 reader 或者 writer 上均可，此处选择在 reader 上进行 fake）
            for (Configuration conf : contentConfig) {
                conf.set(JOB_READER_PARAMETER + "." +
                        LOAD_BALANCE_RESOURCE_MARK, "aFakeResourceMarkForLoadBalance");
            }
            // 是为了避免某些插件没有设置 资源标识 而进行了一次随机打乱操作
            Collections.shuffle(contentConfig, new SecureRandom());
        }

        LinkedHashMap<String, List<Integer>> resourceMarkAndTaskIdMap = parseAndGetResourceMarkAndTaskIdMap(contentConfig);
        List<Configuration> taskGroupConfig = doAssign(resourceMarkAndTaskIdMap, configuration, taskGroupNumber);

        // 调整 每个 taskGroup 对应的 Channel 个数（属于优化范畴）
        adjustChannelNumPerTaskGroup(taskGroupConfig, channelNumber);
        return taskGroupConfig;
    }

    private static void adjustChannelNumPerTaskGroup(List<Configuration> taskGroupConfig, int channelNumber) {
        int taskGroupNumber = taskGroupConfig.size();
        int avgChannelsPerTaskGroup = channelNumber / taskGroupNumber;
        int remainderChannelCount = channelNumber % taskGroupNumber;
        // 表示有 remainderChannelCount 个 taskGroup,其对应 Channel 个数应该为：avgChannelsPerTaskGroup + 1；
        // （taskGroupNumber - remainderChannelCount）个 taskGroup,其对应 Channel 个数应该为：avgChannelsPerTaskGroup

        int i = 0;
        for (; i < remainderChannelCount; i++) {
            taskGroupConfig.get(i).set(CORE_CONTAINER_TASK_GROUP_CHANNEL, avgChannelsPerTaskGroup + 1);
        }

        for (int j = 0; j < taskGroupNumber - remainderChannelCount; j++) {
            taskGroupConfig.get(i + j).set(CORE_CONTAINER_TASK_GROUP_CHANNEL, avgChannelsPerTaskGroup);
        }
    }

    /**
     * 根据task 配置，获取到：
     * 资源名称到 taskId(List) 的 map 映射关系
     *
     * @param contentConfig configuration
     * @return hashmap
     */
    private static LinkedHashMap<String, List<Integer>> parseAndGetResourceMarkAndTaskIdMap(List<Configuration> contentConfig) {
        // key: resourceMark, value: taskId
        LinkedHashMap<String, List<Integer>> readerResourceMarkAndTaskIdMap = new LinkedHashMap<>();
        LinkedHashMap<String, List<Integer>> writerResourceMarkAndTaskIdMap = new LinkedHashMap<>();

        for (Configuration aTaskConfig : contentConfig) {
            int taskId = aTaskConfig.getInt(CoreConstant.TASK_ID);
            // 把 readerResourceMark 加到 readerResourceMarkAndTaskIdMap 中
            String readerResourceMark = aTaskConfig.getString(JOB_READER_PARAMETER + "." + LOAD_BALANCE_RESOURCE_MARK);
            readerResourceMarkAndTaskIdMap.computeIfAbsent(readerResourceMark, k -> new ArrayList<>());
            readerResourceMarkAndTaskIdMap.get(readerResourceMark).add(taskId);

            // 把 writerResourceMark 加到 writerResourceMarkAndTaskIdMap 中
            String writerResourceMark = aTaskConfig.getString(JOB_WRITER_PARAMETER + "." + LOAD_BALANCE_RESOURCE_MARK);
            writerResourceMarkAndTaskIdMap.computeIfAbsent(writerResourceMark, k -> new ArrayList<>());
            writerResourceMarkAndTaskIdMap.get(writerResourceMark).add(taskId);
        }

        if (readerResourceMarkAndTaskIdMap.size() >= writerResourceMarkAndTaskIdMap.size()) {
            // 采用 reader 对资源做的标记进行 shuffle
            return readerResourceMarkAndTaskIdMap;
        } else {
            // 采用 writer 对资源做的标记进行 shuffle
            return writerResourceMarkAndTaskIdMap;
        }
    }

    /**
     * 需要实现的效果通过例子来说是：
     * <pre>
     * a 库上有表：0, 1, 2
     * a 库上有表：3, 4
     * c 库上有表：5, 6, 7
     *
     * 如果有 4个 taskGroup
     * 则 assign 后的结果为：
     * taskGroup-0: 0,  4,
     * taskGroup-1: 3,  6,
     * taskGroup-2: 5,  2,
     * taskGroup-3: 1,  7
     *
     * </pre>
     *
     * @param resourceMarkAndTaskIdMap resource map
     * @param jobConfiguration         configuration
     * @param taskGroupNumber          the number of group
     * @return list of configuration
     */
    private static List<Configuration> doAssign(LinkedHashMap<String, List<Integer>> resourceMarkAndTaskIdMap, Configuration jobConfiguration, int taskGroupNumber) {
        List<Configuration> contentConfig = jobConfiguration.getListConfiguration(JOB_CONTENT);

        Configuration taskGroupTemplate = jobConfiguration.clone();
        taskGroupTemplate.remove(JOB_CONTENT);

        List<Configuration> result = new ArrayList<>();

        List<List<Configuration>> taskGroupConfigList = new ArrayList<>(taskGroupNumber);
        for (int i = 0; i < taskGroupNumber; i++) {
            taskGroupConfigList.add(new ArrayList<>());
        }

        int mapValueMaxLength = -1;

        List<String> resourceMarks = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : resourceMarkAndTaskIdMap.entrySet()) {
            resourceMarks.add(entry.getKey());
            if (entry.getValue().size() > mapValueMaxLength) {
                mapValueMaxLength = entry.getValue().size();
            }
        }

        int taskGroupIndex = 0;
        for (int i = 0; i < mapValueMaxLength; i++) {
            for (String resourceMark : resourceMarks) {
                if (!resourceMarkAndTaskIdMap.get(resourceMark).isEmpty()) {
                    int taskId = resourceMarkAndTaskIdMap.get(resourceMark).get(0);
                    taskGroupConfigList.get(taskGroupIndex % taskGroupNumber).add(contentConfig.get(taskId));
                    taskGroupIndex++;

                    resourceMarkAndTaskIdMap.get(resourceMark).remove(0);
                }
            }
        }
        Configuration tempTaskGroupConfig;
        for (int i = 0; i < taskGroupNumber; i++) {
            tempTaskGroupConfig = taskGroupTemplate.clone();
            tempTaskGroupConfig.set(JOB_CONTENT, taskGroupConfigList.get(i));
            tempTaskGroupConfig.set(CORE_CONTAINER_TASK_GROUP_ID, i);

            result.add(tempTaskGroupConfig);
        }

        return result;
    }
}
