<?php

use Gravita\Http\Database;
use Gravita\Http\Response;
use Gravita\Http\User;

ini_set('error_reporting', E_ALL); // FULL DEBUG 
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);

require_once(__DIR__ . '/../vendor/autoload.php');
$uuid = $_GET["uuid"] ?? null;
if (!$uuid) {
    (new Response())->message("Property uuid not found")->error_and_exit();
}
$db = new Database();
$user = User::get_by_uuid($db, $uuid);
if(!$user) {
    Response::not_found_and_exit();
}
Response::json_response_and_exit(200, $user->to_response());