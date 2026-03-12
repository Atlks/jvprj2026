package filGc;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Pattern;

import static java.lang.System.exit;

public class DuplicateMoverWalk {

    public static void main(String[] args) {
        Path sourceDir = Paths.get("C:\\Users\\attil\\OneDrive\\Documents");
        String targetDirFolder = "C:/dupli";
        Path targetDir = mkdir(targetDirFolder);
        if (targetDir == null) return;

        // 正则匹配 副本(1) ~ 副本(999)
        Pattern pattern = Pattern.compile("\\((\\d{1,3})\\)");

        try {
            // walk 遍历所有文件和子目录
            Files.walk(sourceDir)
                    .filter(Files::isRegularFile) // 只处理文件
                    .forEach(file -> {
                        String fileName = file.getFileName().toString();
                        String fileRelatePath = getRelatePath(file, sourceDir);
                        String targetFile = targetDir + "/" + fileRelatePath;
                        mkdirForFile(targetFile);
                        if (fileName.contains("副本"))
                            movToFile(file, targetFile);
                        if (pattern.matcher(fileName).find()) {
                            movToFile(file, targetFile);
                        }
                        exit(0);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("处理完成！");
    }

    /**
     * 移动文件到目标路径，如果目标路径的父目录不存在，会自动创建
     */
    private static void movToFile(Path file, String targetFile) {
        Path targetPath = Paths.get(targetFile);
        mkdirForFile(targetFile); // 确保父目录存在
        try {
            // 移动文件，如果目标已存在则覆盖
            Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("已移动: " + file + " -> " + targetPath);
        } catch (IOException e) {
            System.err.println("移动失败: " + file);
            e.printStackTrace();
        }
    }

    /**
     * 根据目标文件路径创建父目录
     */
    private static void mkdirForFile(String targetFile) {
        Path targetPath = Paths.get(targetFile);
        Path parentDir = targetPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
                System.out.println("已创建目录: " + parentDir);
            } catch (IOException e) {
                System.err.println("创建目录失败: " + parentDir);
                e.printStackTrace();
            }
        }
    }

    /**
     * 得到 file 相对于 rootDir 的相对路径（字符串形式）
     */
    private static String getRelatePath(Path file, Path rootDir) {
        return rootDir.relativize(file).toString().replace("\\", "/"); // 返回统一斜杠
    }

    @Nullable
    private static Path mkdir(String targetDirFolder) {
        Path targetDir = Paths.get(targetDirFolder);

        // 创建目标文件夹
        if (!Files.exists(targetDir)) {
            try {
                Files.createDirectories(targetDir);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return targetDir;
    }

    private static void movToDir(Path file, Path targetDir) {
        Path targetPath = targetDir.resolve(file.getFileName());
        try {
            Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("已移动: " + file);
        } catch (IOException e) {
            System.err.println("移动失败: " + file);
            e.printStackTrace();
        }
    }
}
