<?php

namespace Gravita\Http\Config;

class Config
{
    public static string $dbsystem = 'pgsql'; //mysql or pgsql
    public static int $port = 5432; // default mysql port 3306 | default pgsql port 5432
    public static string $host = 'localhost'; // database host
    public static string $db = 'db'; // database name
    public static string $user = 'user'; // user name
    public static string $password = 'password'; // password
    public static string $bearerToken = "wyN3h4KPkQrmhANQJpjvsQJx3kkcpgxk"; // CHANGE IT(!)
    public static int $sessionExpireSeconds = 60 * 60 * 1; // 1 hour


    public static $persistent = true;

    public static function getDSN(): string {
        return self::$dbsystem . ":host=" . self::$host . ";port=" . self::$port . ";dbname=" . self::$db . ";";
    }
}
