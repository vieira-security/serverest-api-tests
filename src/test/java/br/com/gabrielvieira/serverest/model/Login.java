package br.com.gabrielvieira.serverest.model;

/** Payload de /login. */
public record Login(String email, String password) {
}
