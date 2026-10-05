import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class GenBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        System.out.println(enc.encode("12345678"));
    }
}
