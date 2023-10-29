<?php

use Gravita\Http\Config\Config;
use Gravita\Http\Database;
use Gravita\Http\Response;
use Gravita\Http\User;
use Gravita\Http\UserSession;
use Gravita\Http\Utils;

// ini_set('error_reporting', E_ALL); // FULL DEBUG 
// ini_set('display_errors', 1);
// ini_set('display_startup_errors', 1);

require_once(__DIR__ . '/../vendor/autoload.php');
// xdebug_break();
$json = Utils::read_json_input();
$username = $json["username"] ?? null;
$uuid = $json["uuid"] ?? null;
$accessToken = $json["accessToken"] ?? null;
$serverId = $json["serverId"] ?? null;
if (!$username && !$uuid) {
    (new Response())->message("Property username or uuid not found")->error_and_exit();
}
if (!$serverId) {
    (new Response())->message("Property serverId not found")->error_and_exit();
}
if (!$accessToken) {
    (new Response())->message("Property accessToken not found")->error_and_exit();
}
if(Utils::get_bearer_token() != Config::$bearerToken) {
    (new Response())->message("Wrong bearer token")->error_and_exit();
}
$db = new Database();
$session = null;
$session = UserSession::get_by_access_token_with_user($db, $accessToken);
if(!$session) {
    (new Response())->message("session not found")->error_and_exit();
}
if($session->access_token !== $accessToken) {
    (new Response())->message("accessToken incorrect")->error_and_exit();
}
if($username && $session->user->username !== $username) {
    (new Response())->message("username incorrect")->error_and_exit();
} else if($uuid && $session->user->uuid !== $uuid) {
    (new Response())->message("uuid incorrect")->error_and_exit();
}
$session->update_server_id($db, $serverId);
Response::json_response_and_exit(200, []);