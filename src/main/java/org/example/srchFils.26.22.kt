package ztoolsMy

import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

fun main() {
    val mdfDate = "2020-11-11"

    val modifiedAfter = LocalDateTime.parse("${mdfDate}T00:00:00")

    var sourceDirPath = "C:\\Users\\attil\\OneDrive\\Documents\\SQL Server Management Studio 22"
    var targetDirPath = "bkPrjDBS241112asrch"
    val extnames = "sql"
    val srchKwds = "InsertTx"

    srchFL(modifiedAfter, extnames, sourceDirPath, targetDirPath, srchKwds)
}

/**
 * 搜索并处理指定目录中修改日期在指定时间之后的文件
 * - 支持按扩展名筛选（例如 java, xml）
 * - 支持按关键字内容匹配
 * - 复制保留原始目录结构（可选）
 *
 * @param modifiedAfter 修改时间必须晚于此时间
 * @param extnames 扩展名筛选，例如："java,xml"
 * @param sourceDirPath 源文件目录路径
 * @param targetDirPath 匹配文件要复制到的目标路径（保留目录结构）
 * @param srchKwds 要搜索的关键词（用空格分隔，全部都必须匹配）
 */
fun srchFL(
    modifiedAfter: LocalDateTime,
    extnames: String,
    sourceDirPath: String,
    targetDirPath: String,
    srchKwds: String
) {
    val sourceDir = File(sourceDirPath)
    val targetDir = File(targetDirPath)

    if (!sourceDir.exists()) {
        throw Exception("主目录不存在：$sourceDirPath")
    }

    targetDir.mkdirs()

    sourceDir.walkTopDown().forEach { entity ->
        if (!entity.isFile) return@forEach

        // 排除目录过滤（可扩展）
        if (entity.path.contains("/vendor/") || entity.path.contains("/node_modules/")) return@forEach

        val modifiedDate = LocalDateTime.ofInstant(entity.lastModified().toInstant(), ZoneId.systemDefault())
        if (!modifiedDate.isAfter(modifiedAfter)) return@forEach

        val extension = entity.extension
        if (extension.isEmpty() || !isInSet(extension, extnames)) return@forEach

        val relativePath = entity.path.removePrefix(sourceDir.path)
        val targetFile = File(targetDir.path + relativePath)
        targetFile.parentFile.mkdirs()

        val txt = try {
            entity.readText()
        } catch (e: Exception) {
            println("读取文件出错: ${e.message}")
            return@forEach
        }

        if (containsAllKeywords(txt, srchKwds)) {
            println("findok..")
            println(entity)
            // entity.copyTo(targetFile, overwrite = true)
            // println("已复制文件: ${entity.path} 到 ${targetFile.path}")
        }
    }
}

fun containsAllKeywords(txt: String, keywordsStr: String): Boolean {
    val keywords = keywordsStr.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
    return keywords.all { txt.contains(it) }
}

fun isInSet(ext: String, extnames: String): Boolean {
    return extnames.split(",").map { it.trim() }.contains(ext)
}

// 拓展函数，简化 File -> Instant 转换
fun Long.toInstant() = java.time.Instant.ofEpochMilli(this)
