<?php

namespace Gravita\Http;

class Response {
    private $httpCode;
    private $errorCode;
    private $errorMessage;
    public function __construct() {
        $this->httpCode = 400;
        $this->errorCode = 0;
        $this->errorMessage = "Unknown error";
    }

    public function http($newValue) : Response {
        $this->httpCode = $newValue;
        return $this;
    }

    public function code($newValue) : Response {
        $this->errorCode = $newValue;
        return $this;
    }

    public function message($newValue) : Response {
        $this->errorMessage = $newValue;
        return $this;
    }

    public function error_and_exit() {
        Response::json_response_and_exit($this->httpCode, [
            "error" => $this->errorMessage,
            "code" => $this->errorCode
        ]);
    }

    public static function json_response_and_exit($code, $data)
    {
        http_response_code($code);
        header("Content-Type: application/json");
        echo json_encode($data);
        exit(0);
    }

    public static function not_found_and_exit() {
        http_response_code(404);
        exit(0);
    }
}