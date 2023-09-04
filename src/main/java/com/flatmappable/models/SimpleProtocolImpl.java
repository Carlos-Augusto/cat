package com.flatmappable.models;

import com.ubirch.crypto.PrivKey;
import com.ubirch.protocol.Protocol;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.*;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class SimpleProtocolImpl extends Protocol {

    private final UUID clientUUID;
    private final PrivKey clientKey;
    private byte[] lastSignature = null;

    public  SimpleProtocolImpl(UUID clientUUID, PrivKey clientKey) {
        this.clientKey = clientKey;
        this.clientUUID = clientUUID;
    }

    @Override
    protected byte[] getLastSignature(UUID uuid) {
        if (lastSignature == null) {
            return new byte[64];
        } else {
            return lastSignature;
        }
    }

    @Override
    public byte[] sign(UUID uuid, byte[] data, int offset, int len) throws SignatureException, InvalidKeyException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512", BouncyCastleProvider.PROVIDER_NAME);
            digest.update(data, offset, len);

            if (uuid.equals(clientUUID)) {
                lastSignature = clientKey.sign(digest.digest());
                // here would be a good place to store the last generated signature before returning
                return lastSignature;
            }
            throw new InvalidKeyException(String.format("unknown uuid: %s", uuid.toString()));

        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new SignatureException(e);
        }
    }

    @Override
    public boolean verify(UUID uuid, byte[] data, int offset, int len, byte[] signature) throws SignatureException, InvalidKeyException {

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                digest.update(data, offset, len);
                return clientKey.verify(digest.digest(), signature);
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new SignatureException(e);
            }


    }
}
