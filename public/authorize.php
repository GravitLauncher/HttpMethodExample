<?php

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
$login = $json["login"] ?? null;
$password = $json["password"] ?? null;
$totpCode = $json["totpCode"] ?? null;
if (!$login) {
    (new Response())->message("Property login not found")->error_and_exit();
}
if (!$password) {
    (new Response())->message("Property password not found")->error_and_exit();
}
$db = new Database();
$user = User::get_by_username($db, $login);
if(!$user) {
    (new Response())->message("auth.usernotfound")->error_and_exit();
}
if(!$user->verify_password($password)) {
    (new Response())->message("auth.wrongpassword")->error_and_exit();
}
if(false /* you can implement check: user enabled 2FA */) {
    if($totpCode) {
        if(true /* you can implement check: totp code is "wrong"*/) {
            (new Response())->message("auth.wrongtotp")->error_and_exit();
        }
    } else {
        (new Response())->message("auth.require2fa")->error_and_exit();
    }
}
$session = UserSession::create_for_user($db, $user->id);

Response::json_response_and_exit(200, [
    "accessToken" => $session->access_token,
    "refreshToken" => $session->refresh_token,
    "id" => $session->id,
    "expire" => $session->expire_in,
    "user" => $user->to_response()
]);