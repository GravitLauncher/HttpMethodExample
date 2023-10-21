<?php

use Gravita\Http\Config\Config;
use Gravita\Http\Database;
use Gravita\Http\Response;
use Gravita\Http\User;
use Gravita\Http\Utils;

ini_set('error_reporting', E_ALL); // FULL DEBUG 
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);

require_once(__DIR__ . '/../vendor/autoload.php');
$uuid = $_GET["uuid"] ?? null;
if (!$uuid) {
    (new Response())->message("Property uuid not found")->error_and_exit();
}
if(Utils::get_bearer_token() != Config::$bearerToken) {
    (new Response())->message("Wrong bearer token")->error_and_exit();
}
$db = new Database();
$user = User::get_by_uuid($db, $uuid);
if(!$user) {
    Response::not_found_and_exit();
}
Response::json_response_and_exit(200, $user->to_response());