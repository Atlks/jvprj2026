 
REM 使用 Windows 身份验证执行备份
rem sqlcmd -S localhost\SQLEXPRESS -E -i "D:\scrpt\bk.sql"


 
setlocal

REM 获取当前日期 (格式: YYYYMMDD)
set DATESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%

REM 执行备份，文件名带日期
sqlcmd -S localhost -E -Q "BACKUP DATABASE [hxMatching] TO DISK = 'D:\Backup\hxMatching_Full_%DATESTAMP%.bak' WITH INIT, STATS = 10;"
sqlcmd -S localhost -E -Q "BACKUP DATABASE [hxMatching] TO DISK = 'z:\hxMatching_Full_%DATESTAMP%.bak' WITH INIT, STATS = 10;"

endlocal


REM 如果使用 SQL 登录账号，请改为：
REM sqlcmd -S localhost\SQLEXPRESS -U sa -P yourpassword -i "C:\SQLScripts\FullBackup.sql"


//10.0.100.12/share2026