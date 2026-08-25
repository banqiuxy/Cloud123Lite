package com.banqiu.thirdparty123pan.util

import java.util.UUID

/**
 * 设备指纹生成（API.md §9）：
 * - loginuuid：UUID v4，会话唯一标识，持久化保持设备一致性
 * - devicetype：随机小米设备型号
 * - osversion：随机 Android 系统版本
 */
object DeviceFingerprint {

    private val osVersions = listOf(
        "Android_4.1.2", "Android_4.2.2", "Android_4.3", "Android_4.4.4",
        "Android_5.1.1", "Android_6.0.1", "Android_7.1.2", "Android_8.0.0",
        "Android_8.1.0", "Android_9.0", "Android_10", "Android_11",
        "Android_12", "Android_13"
    )

    private val deviceTypes = listOf(
        "M2006C3MI", "211033MI", "220333QPG", "M2102J2SC", "2107119DC",
        "2201122C", "23049PCD8G", "M2012K11AC", "2206122SC", "23127PN0CC",
        "2108119RG", "22081283C", "M2101K9C", "24031PN0DC", "23013RK37C",
        "M2007J3SY", "21121210AC", "2201123C", "2210132C", "2312DRA50C"
    )

    fun newUuid(): String = UUID.randomUUID().toString()

    fun randomOsVersion(): String = osVersions.random()

    fun randomDeviceType(): String = deviceTypes.random()

    fun buildUserAgent(osVersion: String): String = "123pan/v2.4.0($osVersion;Xiaomi)"
}