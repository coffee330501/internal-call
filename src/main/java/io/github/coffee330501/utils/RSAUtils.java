package io.github.coffee330501.utils;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * RSA非对称加密
 */
public class RSAUtils {
    /**
     * RSA最大加密明文大小
     */
    private static final int MAX_ENCRYPT_BLOCK = 117;
    /**
     * RSA最大解密密文大小
     */
    private static final int MAX_DECRYPT_BLOCK = 128;
    public static final String RSA_ALGORITHM = "RSA";
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private static BouncyCastleProvider bouncyCastleProvider = null;

    private RSAUtils() {
    }

    public static Map<String, String> createKeys() {

        Map<String, String> keyPairMap = new HashMap<>();
        KeyPairGenerator keyPairGenerator;
        Provider provider = getInstance();
        Security.addProvider(provider);
        try {

            /* step1 ： 基于RSA算法生成keyPairGenerator对象
             *
             * 如果默认的提供程序（provider）提供RSA算法的实现，则返回包含该实现的 KeyPairGenerator 的实例。
             * 如果默认包中不存在该算法，则搜索其他包。
             *
             * 也可以指定其它加密算法提供程序 ， 如 BC：
             * KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());
             */
            keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM, provider);

            /* step2：初始化keyPairGenerator密钥对生成器 ， 确定密钥长度大小与随机源
             *
             * 使用默认的参数集合并使用提供程序（以最高优先级安装）的 SecureRandom 实现作为随机源。
             * 如果任何安装的提供程序都不提供 SecureRandom 的实现，则使用系统提供的随机源。
             *
             * 当然也可以指定随机源，如下：
             * keyPairGenerator.initialize(1024,new SecureRandom(byte[] bytes));
             */
            keyPairGenerator.initialize(1024, new SecureRandom());
            //keyPairGenerator.initialize(1024,new SecureRandom(DateFormatUtils.format(new Date(),"yyyyMMdd").getBytes()));

            // step3：获取密钥对
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            // step4：获取密钥
            String privateKey = Base64.encodeBase64URLSafeString(keyPair.getPrivate().getEncoded());
            String publicKey = Base64.encodeBase64URLSafeString(keyPair.getPublic().getEncoded());
            keyPairMap.put("privateKey", privateKey);
            keyPairMap.put("publicKey", publicKey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("生成密钥对失败：" + e);
        }

        return keyPairMap;
    }

    /**
     * 从公钥字符串中得到公钥
     *
     * @param publicKey 密钥字符串（经过base64编码）
     * @throws Exception
     */
    public static PublicKey getPublicKey(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 通过X509编码的Key指令获得公钥对象
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
        return keyFactory.generatePublic(x509KeySpec);
    }

    /**
     * 从私钥字符串中得到私钥
     *
     * @param privateKey 密钥字符串（经过base64编码）
     * @throws Exception
     */
    public static PrivateKey getPrivateKey(String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 通过PKCS#8编码的Key指令获得私钥对象
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
        return keyFactory.generatePrivate(pkcs8KeySpec);
    }

    /**
     * 公钥加密 分段加密
     * <p>
     * RSA 加解密时，对加密的数据大小有限制，最大不大于密钥长度。
     * <p>
     * 在使用 1024 位的密钥时,最大可以加密 1024/8 = 128字节的数据，
     * 此时需要对数据进行分组加密，分组加密后的加密串拼接成一个字符串返回给客户端。
     * 如果 Padding 方式使用默认的 OPENSSL_PKCS1_PADDING(需要占用11字节用于填充)，
     * 则明文长度最多为 128 - 11 = 117 Bytes
     */
    public static String encrypt(String plainText, String publicKeyStr) throws Exception {
        byte[] plainTextArray = plainText.getBytes();
        PublicKey publicKey = getPublicKey(publicKeyStr);
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        int inputLen = plainTextArray.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        int i = 0;
        byte[] cache;
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(plainTextArray, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(plainTextArray, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptText = out.toByteArray();
        out.close();
        return new String(Base64.encodeBase64(encryptText));
    }

    /**
     * 分段解密
     */
    public static String decrypt(String encryptTextHex, String privateKeyStr) throws Exception {
        byte[] encryptText = Base64.decodeBase64(encryptTextHex);
        PrivateKey privateKey = getPrivateKey(privateKeyStr);
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        int inputLen = encryptText.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(encryptText, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(encryptText, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        out.close();
        return out.toString();
    }

    /**
     * RSA私钥签名
     *
     * @param content    待签名数据
     * @param privateKey 私钥
     * @return 签名值
     */
    public static String signByPrivateKey(String content, String privateKey) {
        try {
            PrivateKey priKey = getPrivateKey(privateKey);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(priKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            byte[] signed = signature.sign();
            return new String(Base64.encodeBase64URLSafe(signed), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过公钥验签
     *
     * @param content   验签内容
     * @param sign      签名
     * @param publicKey 公钥
     * @return 验签结果
     */
    public static boolean verifySignByPublicKey(String content, String sign, String publicKey) {
        try {
            PublicKey pubKey = getPublicKey(publicKey);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(pubKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.decodeBase64(sign.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 单例模式取得BouncyCastleProvider 避免内存泄漏
     *
     * @return
     */
    public static synchronized BouncyCastleProvider getInstance() {
        if (bouncyCastleProvider == null) {
            bouncyCastleProvider = new BouncyCastleProvider();
        }
        return bouncyCastleProvider;
    }
}