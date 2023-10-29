<?php

use Gravita\Http\Config\Config;
use Gravita\Http\Database;
use Gravita\Http\Response;
use Gravita\Http\UserSession;
use Gravita\Http\Utils;

ini_set('error_reporting', E_ALL); // FULL DEBUG 
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);

require_once(__DIR__ . '/../vendor/autoload.php');
// xdebug_break();
$json = Utils::read_json_input();
$refreshToken = $json["refreshToken"] ?? null;
if (!$refreshToken) {
    (new Response())->message("Property refreshToken not found")->error_and_exit();
}
if(Utils::get_bearer_token() != Config::$bearerToken) {
    (new Response())->message("Wrong bearer token")->error_and_exit();
}
$db = new Database();
$session = UserSession::get_by_refresh_token($db, $refreshToken);
if(!$session) {
    (new Response())->code(1002)->message("auth.invalidtoken")->error_and_exit();
}
$session->refresh($db);
Response::json_response_and_exit(200, [
    "accessToken" => $session->access_token,
    "refreshToken" => $session->refresh_token,
    "id" => $session->id,
    "expire" => $session->expire_in - time()
]);