<?php

namespace Gravita\Http;
use Gravita\Http\Database;
use \PDO;

class User {
    public $id;
    public $username;
    public $uuid;
    private $password_hash;

    public function __construct($id = null, $username = null, $uuid = null, $password_hash = null) {
        $this->id = $id;
        $this->username = $username;
        $this->uuid = $uuid;
        $this->password_hash = $password_hash;
    }

    public function verify_password($password) {
        return password_verify($password, $this->password_hash);
    }

    public function to_response() {
        return [
            "username" => $this->username,
            "uuid" => $this->uuid,
            "roles" => [],
            "permissions" => [],
            "assets" => (object) [], // You can implement assets
            "properties" => (object) []
        ];
    }

    public static function get_by_id(Database $db, $id) : User|null {
        $stmt = $db->getPDO()->prepare("SELECT * FROM users WHERE id=:id");
        $stmt->execute(['id' => $id]);
        return User::read_from_row($stmt->fetch(PDO::FETCH_ASSOC));
    }

    public static function get_by_uuid(Database $db, $uuid) : User|null {
        $stmt = $db->getPDO()->prepare("SELECT * FROM users WHERE uuid=:uuid");
        $stmt->execute(['uuid' => $uuid]);
        return User::read_from_row($stmt->fetch(PDO::FETCH_ASSOC));
    }

    public static function get_by_username(Database $db, $username) : User|null {
        $stmt = $db->getPDO()->prepare("SELECT * FROM users WHERE username=:username");
        $stmt->execute(['username' => $username]);
        return User::read_from_row($stmt->fetch(PDO::FETCH_ASSOC));
    }

    public static function read_from_row($row) : User|null {
        if(!$row) {
            return null;
        }
        $user = new User();
        $user->id = $row["id"];
        $user->username = $row["username"];
        $user->uuid = $row["uuid"];
        $user->password_hash = $row["password"];
        return $user;
    }
}