if (-not ("DiskCacheInfo" -as [type])) {
Add-Type -TypeDefinition Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

public class DiskCacheInfo {
    [StructLayout(LayoutKind.Sequential)]
    public struct DISK_CACHE_INFORMATION {
        public bool ParametersSavable;
        public bool ReadCacheEnabled;
        public bool WriteCacheEnabled;
        public bool ReadRetentionPriority;
        public bool WriteRetentionPriority;
        public byte DisablePrefetchTransferLength;
        public byte PrefetchScalar;
        public uint PrefetchMinimum;
        public uint PrefetchMaximum;
        public uint CacheSize;
        public bool CacheEnabled;
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern IntPtr CreateFile(
        string lpFileName,
        uint dwDesiredAccess,
        uint dwShareMode,
        IntPtr lpSecurityAttributes,
        uint dwCreationDisposition,
        uint dwFlagsAndAttributes,
        IntPtr hTemplateFile);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool DeviceIoControl(
        IntPtr hDevice,
        uint dwIoControlCode,
        IntPtr lpInBuffer,
        uint nInBufferSize,
        out DISK_CACHE_INFORMATION lpOutBuffer,
        uint nOutBufferSize,
        out uint lpBytesReturned,
        IntPtr lpOverlapped);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool CloseHandle(IntPtr hObject);

    public const uint GENERIC_READ = 0x80000000;
    public const uint FILE_SHARE_READ = 0x00000001;
    public const uint FILE_SHARE_WRITE = 0x00000002;
    public const uint OPEN_EXISTING = 3;
    public const uint IOCTL_DISK_GET_CACHE_INFORMATION = 0x003940D8;

    public static bool GetWriteCacheStatus(int diskNumber) {

        string path = @"\\.\PhysicalDrive" + diskNumber;

        IntPtr hDevice = CreateFile(path, GENERIC_READ, FILE_SHARE_READ | FILE_SHARE_WRITE,
            IntPtr.Zero, OPEN_EXISTING, 0, IntPtr.Zero);

        if (hDevice.ToInt64() == -1) {
            throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error());
        }

        DISK_CACHE_INFORMATION cacheInfo;
        uint bytesReturned;
        bool result = DeviceIoControl(hDevice, IOCTL_DISK_GET_CACHE_INFORMATION,
            IntPtr.Zero, 0, out cacheInfo, (uint)Marshal.SizeOf(typeof(DISK_CACHE_INFORMATION)),
            out bytesReturned, IntPtr.Zero);

        CloseHandle(hDevice);

        if (!result) {
            throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error());
        }

        return cacheInfo.WriteCacheEnabled;
    }
}
"@
}





# 自动枚举所有磁盘
$disks = Get-WmiObject -Class Win32_DiskDrive | Select-Object Index,Model

foreach ($disk in $disks) {
    try {
        $status = [DiskCacheInfo]::GetWriteCacheStatus($disk.Index)
        Write-Output "PhysicalDrive$($disk.Index) ($($disk.Model)) Write Cache Enabled: $status"
    } catch {
        Write-Output "PhysicalDrive$($disk.Index) ($($disk.Model)) - Error: $($_.Exception.Message)"
    }
}
