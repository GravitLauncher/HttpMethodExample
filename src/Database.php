<?php
namespace Gravita\Http;
use Gravita\Http\Config\Config;
use \PDO;

class Database {
    private $dsn;
    private PDO $pdo;

    public function __construct() {
        $this->dsn = Config::getDSN();
        $this->pdo = $this->pdo = new PDO(Config::getDSN(), Config::$user, Config::$password, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_PERSISTENT => Config::$persistent]);
    }

    public function getPDO() : PDO {
        return $this->pdo;
    }
}