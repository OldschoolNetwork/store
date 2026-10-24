package com.oldschoolnetwork.core;

interface IONCoreService {
    // Системные данные
    String getCoreStatus();
    int getCoreVersion();
    boolean isLoggedIn();
    boolean hasRoot();

    // Данные пользователя (Getters)
    String getEmail();
    String getUsername();
    String getName();
    String getSurname();
    String getAtId();
    String getOnId();
    String getToken();
    String getAvatar();
    String getPhoneNumber(); // Задел на будущее!

    // Обновление данных (Setters)
    void setAvatar(in String base64Image);
    void updateProfileInfo(in String email, in String name, in String surname, in String username, in String phoneNumber);

    // Управление сессией
    void logout();
}
