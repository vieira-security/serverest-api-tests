package br.com.gabrielvieira.serverest.model;

/** Payload de /usuarios. A ServeRest espera "administrador" como string ("true"/"false"). */
public record Usuario(String nome, String email, String password, String administrador) {
}
