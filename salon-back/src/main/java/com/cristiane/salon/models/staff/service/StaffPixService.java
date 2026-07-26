package com.cristiane.salon.models.staff.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.staff.dto.GenerateStaffPixRequest;
import com.cristiane.salon.models.staff.dto.StaffPixQrCodeResponse;
import com.cristiane.salon.models.staff.entity.StaffProfile;
import com.cristiane.salon.models.staff.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera o QR Code para pagar um membro da equipe via PIX, sem que quem paga (ou qualquer log,
 * response, ou tela) chegue a ver a chave PIX em texto claro.
 *
 * <p>A chave é decifrada só dentro deste método, existe como variável local Java (nunca vira
 * campo de objeto, nunca é serializada, nunca é logada) e é descartada assim que o payload é
 * montado. Isso é o que sustenta a decisão de produto: "cifra a chave, e na hora de pagar
 * gera um QR sem mostrar a chave para quem está pagando".
 */
@Service
@RequiredArgsConstructor
public class StaffPixService {

    private final StaffProfileRepository staffProfileRepository;

    @Transactional(readOnly = true)
    public StaffPixQrCodeResponse generateQrCode(Long staffProfileId, GenerateStaffPixRequest request) {
        StaffProfile profile = staffProfileRepository.findById(staffProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Cadastro de equipe não encontrado"));

        if (!profile.hasPixKey()) {
            throw new BadRequestException("Esta pessoa não tem chave PIX cadastrada");
        }

        // A chave decifrada vive só neste escopo local — não é atribuída a nenhum campo,
        // não entra em log e é descartada ao final do método.
        String decryptedPixKey = profile.getPixKey();

        String txId = "STAFF" + profile.getId();
        String payload = PixBrCodeBuilder.build(
                decryptedPixKey,
                profile.getDisplayName(),
                profile.getCity(),
                request.amount(),
                txId
        );

        return new StaffPixQrCodeResponse(payload, request.amount(), profile.getDisplayName());
    }
}
