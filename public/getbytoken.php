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
// xdebug_break();
$json = Utils::read_json_input();
$accessToken = $json["accessToken"] ?? null;
if (!$accessToken) {
    (new Response())->message("Property access_token not found")->error_and_exit();
}
if(Utils::get_bearer_token() != Config::$bearerToken) {
    (new Response())->message("Wrong bearer token")->error_and_exit();
}
$db = new Database();
$session = UserSession::get_by_access_token_with_user($db, $accessToken);
if(!$session || $session->expire_in < time()) {
    (new Response())->code(1001)->message("auth.tokenexpired")->error_and_exit();
}

Response::json_response_and_exit(200, [
    "accessToken" => $session->access_token,
    "refreshToken" => $session->refresh_token,
    "id" => $session->id,
    "expire" => $session->expire_in,
    "user" => $session->user->to_response()
]);