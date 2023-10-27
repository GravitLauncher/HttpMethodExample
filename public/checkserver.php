<?php

use Gravita\Http\Config\Config;
use Gravita\Http\Database;
use Gravita\Http\Response;
use Gravita\Http\User;
use Gravita\Http\UserSession;
use Gravita\Http\Utils;

ini_set('error_reporting', E_ALL); // FULL DEBUG 
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);

require_once(__DIR__ . '/../vendor/autoload.php');
$json = Utils::read_json_input();
$username = $json["username"] ?? null;
$serverId = $json["serverId"] ?? null;
if (!$username) {
    (new Response())->message("Property username not found")->error_and_exit();
}
if (!$serverId) {
    (new Response())->message("Property serverId not found")->error_and_exit();
}
if(Utils::get_bearer_token() != Config::$bearerToken) {
    (new Response())->message("Wrong bearer token")->error_and_exit();
}
$db = new Database();
$session = UserSession::get_by_server_id_and_username($db, $username, $serverId);
if(!$session) {
    Response::not_found_and_exit();
}
if(!$session->server_id !== $serverId) {
    (new Response())->message("server_id incorrect")->error_and_exit();
}
Response::json_response_and_exit(200, $user->to_response());