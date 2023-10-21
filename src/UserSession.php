<?php
 
namespace Gravita\Http;
 
use Gravita\Http\Config\Config;
use Gravita\Http\Database;
use Gravita\Http\Utils;
use \PDO;
 
class UserSession
{
    public $user;
 
    public function __construct(
        public $id = null,
        public $user_id = null,
        public $access_token = null,
        public $refresh_token = null,
        public $server_id = null,
        public $expire_in = null
    ) {
    }
 
    public function to_response()
    {
        return [
            "id" => $this->id,
            "user_id" => $this->user_id,
            "access_token" => $this->access_token,
            "refresh_token" => $this->refresh_token,
            "server_id" => $this->server_id,
            "expire_in" => $this->expire_in
        ];
    }
 
    public function refresh(Database $db)
    {
        $stmt = $db->getPDO()->prepare("UPDATE user_sessions SET access_token=:access_token, refresh_token=:refresh_token WHERE id=:id");
        $stmt->execute([
            'id' => $this->id,
            'access_token' => $this->access_token = Utils::generate_token(),
            'refresh_token' => $this->refresh_token = Utils::generate_token()
        ]);
    }
 
    public function update_server_id(Database $db, $server_id)
    {
        $this->server_id = $server_id;
        $stmt = $db->getPDO()->prepare("UPDATE user_sessions SET server_id=:server_id WHERE id=:id");
        $stmt->execute([
            'id' => $this->id,
            'server_id' => $this->server_id
        ]);
    }
 
    public static function create_for_user(Database $db, $user_id): UserSession
    {
        $session = new UserSession(null, $user_id, Utils::generate_token(), Utils::generate_token(), null, date("c", time() + Config::$sessionExpireSeconds));
        $stmt = $db->getPDO()->prepare(
            "INSERT INTO user_sessions (user_id,access_token,refresh_token,expire_in)
            VALUES (:user_id, :access_token, :refresh_token, :expire_in)"
        );
        $stmt->execute([
            'user_id' => $user_id,
            'access_token' => $session->access_token,
            'refresh_token' => $session->refresh_token,
            'expire_in' => $session->expire_in
        ]);
        $session->id = $db->getPDO()->lastInsertId();
        $session->expire_in = (int) date("U", strtotime($session->expire_in));
        return $session;
    }
 
    public static function get_by_id(Database $db, $id): UserSession|null
    {
        $stmt = $db->getPDO()->prepare("SELECT * FROM user_sessions WHERE id=:id");
        $stmt->execute(['id' => $id]);
        return UserSession::read_from_row($stmt->fetch(PDO::FETCH_ASSOC));
    }
 
    public static function get_by_access_token_with_user(Database $db, $access_token): UserSession|null
    {
        $stmt = $db->getPDO()->prepare("SELECT user_sessions.id as session_id, users.id as id, username, uuid, access_token, refresh_token, server_id, expire_in, user_id, password FROM user_sessions JOIN users ON user_sessions.user_id=users.id WHERE access_token=:access_token");
        $stmt->execute(['access_token' => $access_token]);
        return UserSession::read_from_row($stmt->fetch(PDO::FETCH_ASSOC), true);
    }
 
    public static function get_by_refresh_token(Database $db, $refresh_token): UserSession|null
    {
        $stmt = $db->getPDO()->prepare("SELECT * FROM user_sessions WHERE refresh_token=:refresh_token");
        $stmt->execute(['refresh_token' => $refresh_token]);
        return UserSession::read_from_row($stmt->fetch(PDO::FETCH_ASSOC));
    }
 
    public static function get_by_server_id_and_username(Database $db, $server_id, $username): UserSession|null
    {
        $stmt = $db->getPDO()->prepare("SELECT user_sessions.id as session_id, users.id as id, username, uuid, access_token, refresh_token, server_id, expire_in, user_id, password FROM user_sessions JOIN users ON user_sessions.user_id = users.id  WHERE server_id=:server_id AND username=:username");
        $stmt->execute(['server_id' => $server_id, 'username' => $username]);
        return UserSession::read_from_row($stmt->fetch(PDO::FETCH_ASSOC), true);
    }
 
    public static function get_by_server_id_and_uuid(Database $db, $server_id, $uuid): UserSession|null
    {
        $stmt = $db->getPDO()->prepare(
            "SELECT user_sessions.id as session_id, users.id as id, username, uuid, access_token, refresh_token, server_id, expire_in, user_id, password
            FROM user_sessions
            JOIN users ON user_sessions.user_id = users.id  WHERE server_id=:server_id AND uuid=:uuid"
        );
        $stmt->execute(['server_id' => $server_id, 'uuid' => $uuid]);
        return UserSession::read_from_row($stmt->fetch(PDO::FETCH_ASSOC), true);
    }
 
    public static function read_from_row($row, bool $enableUser = false): UserSession|null
    {
        if (!$row) {
            return null;
        }
        $session = new UserSession();
        $session->id = $enableUser ? $row['session_id'] : $row['id'];
        $session->user_id = $row['user_id'];
        $session->access_token = $row['access_token'];
        $session->refresh_token = $row['refresh_token'];
        $session->server_id = $row['server_id'];
        $session->expire_in = (int)date("U", strtotime($row['expire_in']));
        if ($enableUser) {
            $session->user = User::read_from_row($row);
        }
        return $session;
    }
}