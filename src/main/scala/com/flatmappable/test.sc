import java.nio.charset.StandardCharsets

import org.apache.commons.codec.binary.Hex
import org.bouncycastle.util.encoders.Base64

Base64.toBase64String("hola".getBytes(StandardCharsets.UTF_8))