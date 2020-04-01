package com.wangyong.controller;

import com.wangyong.utils.HashUtil;

import java.util.*;

public class ConsistentHashingWithVirtualNode {
    /**
     * 集群地址列表
     */
    private static String[] groups = {
            "192.168.0.0:111", "192.168.0.1:111", "192.168.0.2:111",
            "192.168.0.3:111", "192.168.0.4:111"
    };

    /**
     * 真实集群列表
     */
    private static List<String> realGroups = new LinkedList<>();

    /**
     * 虚拟节点映射关系
     */
    private static SortedMap<Integer, String> virtualNodes = new TreeMap<>();

    private static final int VIRTUAL_NODE_NUM = 1000;

    static {
        // 先添加真实节点列表
        realGroups.addAll(Arrays.asList(groups));

        // 将虚拟节点映射到Hash环上   双层for循环加入虚拟节点
        for (String realGroup: realGroups) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                String virtualNodeName = getVirtualNodeName(realGroup, i);
                int hash = HashUtil.getHash(virtualNodeName);
                System.out.println("[" + virtualNodeName + "] launched @ " + hash);
                virtualNodes.put(hash, virtualNodeName);
            }
        }
    }

    private static String getVirtualNodeName(String realName, int num) {
        return realName + "&&VN" + String.valueOf(num);
    }

    private static String getRealNodeName(String virtualName) {
        return virtualName.split("&&")[0];
    }

    private static String getServer(String widgetKey) {
        int hash = HashUtil.getHash(widgetKey);
        // 只取出所有大于该hash值的部分而不必遍历整个Tree
        SortedMap<Integer, String> subMap = virtualNodes.tailMap(hash);
        String virtualNodeName;
        if (subMap == null || subMap.isEmpty()) {
            // hash值在最尾部，应该映射到第一个group上
            virtualNodeName = virtualNodes.get(virtualNodes.firstKey());
        }else {
            virtualNodeName = subMap.get(subMap.firstKey());
        }
        return getRealNodeName(virtualNodeName);
    }

    public static void main(String[] args) {
        // 生成随机数进行测试
        Map<String, Integer> resMap = new HashMap<>();
        // removeGroup("192.168.0.4:111");
        addGroup("192.168.0.44:111");
        for (int i = 0; i < 100000; i++) {
            Integer widgetId = i;
            String group = getServer(widgetId.toString());
            if (resMap.containsKey(group)) {
                resMap.put(group, resMap.get(group) + 1);
            } else {
                resMap.put(group, 1);
            }
        }

        resMap.forEach(
                (k, v) -> {
                    System.out.println("group " + k + ": " + v + "(" + v/100000.0D +"%)");
                }
        );

    }


    private static void refreshHashCircle() {
        // 当集群变动时，刷新hash环，其余的集群在hash环上的位置不会发生变动
        virtualNodes.clear();
        for (String realGroup: realGroups) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                String virtualNodeName = getVirtualNodeName(realGroup, i);
                int hash = HashUtil.getHash(virtualNodeName);
                System.out.println("[" + virtualNodeName + "] launched @ " + hash);
                virtualNodes.put(hash, virtualNodeName);
            }
        }
    }
    private static void addGroup(String identifier) {
        realGroups.add(identifier);
        refreshHashCircle();
    }

    private static void removeGroup(String identifier) {
        int i = 0;
        for (String group:realGroups) {
            if (group.equals(identifier)) {
                realGroups.remove(i);
            }
            i++;
        }
        refreshHashCircle();
    }



}
