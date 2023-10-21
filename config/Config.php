<?php

namespace Gravita\Http\Config;

class Config
{
    public static $dbsystem = "pgsql"; //mysql or pgsql
    public static $port = "5432"; // default mysql port 3306 | default pgsql port 5432
    public static $host = 'localhost'; // database host
    public static $db = 'httpmethod'; // database name
    public static $user = 'httpmethod'; // user name
    public static $password = '1111'; // password
    public static $sessionExpireSeconds = 60*60*1; // 1 hour


    public static $persistent = true;

    public static function getDSN(): string {
        return self::$dbsystem . ":host=" . self::$host . ";port=" . self::$port . ";dbname=" . self::$db . ";";
    }
}
