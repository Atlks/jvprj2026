$Action = New-ScheduledTaskAction -Execute "C:\SQLScripts\RunBackup.bat"
$Trigger = New-ScheduledTaskTrigger -Daily -At 2am
Register-ScheduledTask -TaskName "SQLFullBackup" -Action $Action -Trigger $Trigger -User "DOMAIN\UserName" -Password "Password"
