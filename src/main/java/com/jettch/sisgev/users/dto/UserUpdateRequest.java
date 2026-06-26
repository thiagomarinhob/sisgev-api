package com.jettch.sisgev.users.dto;

import com.jettch.sisgev.users.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Payload de edição de usuário. {@code password} é opcional — quando informada,
 * substitui a senha atual (mínimo 8 caracteres); quando omitida, a senha não é alterada.
 */
public record UserUpdateRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
        String name,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres")
        String email,

        @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
        String password,

        @NotNull(message = "Papel é obrigatório")
        UserRole role,

        UUID municipalityId
) {
}
