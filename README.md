## HttpMethodExample
Это пример реализации собственного модуля и http api к нему
### Ограничения
- accessToken не JWT
- Отдача ассетов(текстур скина и плаща) не реализована
- Не реализован выход из аккаунта
- Не реализован HWID
- Не реализован textureLoader
- Этот проект является примером, в нём отсутствуют механизмы регистрации, смены пароля и т.д. Используйте его для написания собственного кода
- Не реализован 2FA
## Настройка
- В файле `config/Config.php` настройте подключение к БД и **обязательно** смените bearerToken на свой
- Скомплируйте и установите на лаунчсервер модуль `MyHttp_module`
- Настройте способ авторизации
```json
"http": {
      "isDefault": false,
      "core": {
        "userByUsername": "https://example.com/getbyusername.php?username=%username%",
        "userByUuid": "https://example.com/http_method/getbyuuid.php?uuid=%uuid%",
        "userByToken": "https://example.com/http_method/getbytoken.php",
        "refreshAccessToken": "https://example.com/http_method/refreshtoken.php",
        "authorize": "https://example.com/http_method/authorize.php",
        "checkServer": "https://example.com/http_method/checkserver.php",
        "joinServer": "https://example.com/http_method/joinserver.php",
        "bearerToken": "YOUR_BEARER_TOKEN",
        "type": "myhttp"
      },
      "displayName": "Http Method",
      "visible": true
    }
```
- Настройте nginx на отдачу файлов и исполнение php скриптов из подпапки `public`
## Механизм авторизации
Структура `User`
```json
{
  "username": "Gravita",
  "uuid": "bdbfaf45-a921-4721-a370-e9eb7576f60e",
  "permissions": ["launcher.*", "launchserver.*"],
  "roles": ["ADMIN"],
  "assets": {
    "SKIN": {
      "url": "http://example.com/skin.png",
      "digest": "6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b",
      "metadata": {
        "model": "slim"
      }
    }
  }
}
```
Структура `UserSession`
```json
{
  "id": "1",
  "accessToken": "ACCESS_TOKEN",
  "refreshToken": "REFRESH TOKEN",
  "expire": "EXPIRE IN SECONDS",
  "user": {User}
}
```
Ошибка
```json
{
  "error" "Error message",
  "code": 1000
}
```
**Первичная авторизация по логину, паролю и коду 2FA**:  
Запрос POST authorize.php
```json
{
  "login": "Gravita",
  "password": "PASSWORD",
  "totpCode":"000000"
}
```
Ответ: UserSession
- Примечание: totpCode отправляется только если запрошен 2FA
- Для того что бы запросить у пользователя код 2FA нужно в ответе передать ошибку `auth.require2fa`

**Восстановление сессии и вход по сохраненному паролю**:  
Запрос POST getbytoken.php
```json
{
  "accessToken": "ACCESS TOKEN"
}
```
Ответ: `UserSession`

**Обновление истекшего токена**:  
Запрос POST refreshToken.php
```json
{
  "refreshToken": "REFRESH TOKEN"
}
```
Ответ: `UserSession`

**Получение пользователя по username и uuid**:  
Запрос GET getbyusername.php/getbyuuid.php  
Ответ: `User`  

**Вход на сервер (клиент)**
Запрос POST joinserver.php
```json
{
  "username": "Gravita",
  "uuid": "bdbfaf45-a921-4721-a370-e9eb7576f60e",
  "accessToken": "ACCESS TOKEN",
  "serverId": "SERVER ID"
}
```
Ответ: любой с кодом 200

**Проверка входа (сервер)**
Запрос POST checkserver.php
```json
{
  "username" "Gravita",
  "serverId": "SERVER ID"
}
```
Ответ: `User`

### Важные примечания к реализации
- Проверяйте присланный bearerToken - доступ к API должен иметь только лаунчсервер
- При joinServer вы должны сохранить присланный SERVER ID в базу данных, а при checkServer - убедится, что имя пользователя соответствует той сессии, в которой был установлен указанный serverId
- При обновлении токена обязательно вернуть только accessToken, refreshToken и expire. Возвращать каждый раз новый refresh токен необязательно, но рекомендуется
- При смене пароля/2FA удаляйте сессии
- В этой реализации minecraftAccessToken всегда совпадает с oauth access token
