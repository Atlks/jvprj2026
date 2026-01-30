// 编译时需 /clr 开启托管扩展
#include "stdafx.h"
using namespace System;
using namespace System::Data;
using namespace System::Data::SQLite;

int main(array<System::String ^> ^args)
{
    String^ connStr = "Data Source=test.db;Version=3;";
    SQLiteConnection^ conn = gcnew SQLiteConnection(connStr);

    try {
        conn->Open();

        String^ sql = "INSERT INTO users (id, name) VALUES (@id, @name)";
        SQLiteCommand^ cmd = gcnew SQLiteCommand(sql, conn);
        cmd->Parameters->AddWithValue("@id", 1);
        cmd->Parameters->AddWithValue("@name", "Alice");

        cmd->ExecuteNonQuery();
        Console::WriteLine("Insert successful!");
    }
    catch (Exception^ ex) {
        Console::WriteLine("Error: {0}", ex->Message);
    }
    finally {
        conn->Close();
    }

    return 0;
}
