package com.jettch.sisgev.storage;

/** Referência a um objeto armazenado: a key no bucket e a URL pública de acesso. */
public record StoredFile(String key, String url) {
}
