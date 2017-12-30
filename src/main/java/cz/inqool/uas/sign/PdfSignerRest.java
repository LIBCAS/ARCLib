package cz.inqool.uas.sign;

import cz.inqool.uas.file.FileRef;
import cz.inqool.uas.sign.dto.MessageDigest;
import cz.inqool.uas.sign.dto.Signature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Instant;

@ConditionalOnProperty(prefix = "sign", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/api/signer/pdf")
public class PdfSignerRest {
    private PdfSigner signer;

    @RequestMapping(value = "prepare", method = RequestMethod.POST)
    public MessageDigest prepare(@RequestParam("fileId") String fileId,
                                 @RequestParam("certificate") String cert) throws IOException {

        return signer.prepare(fileId, cert);
    }

    @RequestMapping(value = "finalize", method = RequestMethod.POST)
    public FileRef finalize(@RequestParam("fileId") String fileId,
                            @RequestParam("created") Instant created,
                            @RequestParam("signature") String signature,
                            @RequestParam("certificate") String certificate) throws IOException {

        return signer.finalize(fileId, new Signature(created, signature, certificate));
    }

    @Inject
    public void setSigner(PdfSigner signer) {
        this.signer = signer;
    }
}
