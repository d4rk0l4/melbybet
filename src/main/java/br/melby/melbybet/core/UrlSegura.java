package br.melby.melbybet.core;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

/** Regras para aceitar o endereço de uma imagem: só HTTPS, nada de rede interna. Lógica pura (a resolução de DNS é feita por quem baixa). */
public final class UrlSegura {
    private UrlSegura() {}

    public static final int MAX_URL = 500;

    /** Devolve o motivo se o endereço não serve, ou null. Só olha o texto; o IP de cada nome é conferido por {@link #ipPermitido}. */
    public static String validar(String url) {
        if (url == null || url.isBlank()) return "Falta o endereço da imagem.";
        if (url.length() > MAX_URL) return "Endereço grande demais.";
        URI u;
        try {
            u = new URI(url.trim());
        } catch (Exception e) {
            return "Endereço inválido.";
        }
        if (u.getScheme() == null || !u.getScheme().equalsIgnoreCase("https")) return "O endereço precisa começar com https://";
        if (u.getUserInfo() != null) return "Endereços com usuário e senha não são aceitos.";
        String host = u.getHost();
        if (host == null || host.isEmpty()) return "Endereço sem nome de site.";
        if (u.getPort() != -1 && u.getPort() != 443) return "Só a porta padrão (443) é aceita.";
        String h = host.toLowerCase(Locale.ROOT);
        if (h.equals("localhost") || h.endsWith(".localhost") || h.endsWith(".local") || h.endsWith(".internal") || h.endsWith(".lan") || h.endsWith(".home.arpa")) {
            return "Esse endereço aponta para a rede interna.";
        }
        if (!h.contains(".") && !h.contains(":")) return "Esse endereço não parece um site da internet.";
        InetAddress literal = ipLiteral(h);
        if (literal != null && !ipPermitido(literal)) return "Esse endereço aponta para a rede interna.";
        return null;
    }

    /** Se o texto for um IP escrito por extenso (v4 ou v6), devolve-o sem consultar DNS. */
    private static InetAddress ipLiteral(String h) {
        try {
            String s = h.startsWith("[") && h.endsWith("]") ? h.substring(1, h.length() - 1) : h;
            if (s.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
                String[] p = s.split("\\.");
                byte[] b = new byte[4];
                for (int i = 0; i < 4; i++) {
                    int v = Integer.parseInt(p[i]);
                    if (v > 255) return null;
                    b[i] = (byte) v;
                }
                return InetAddress.getByAddress(b);
            }
            if (s.contains(":")) return InetAddress.getByName(s); // só IPv6 literal, sem DNS
        } catch (Exception e) {
            // não é um IP
        }
        return null;
    }

    /** O endereço de uma redireção (que pode ser relativo) em relação ao anterior, já validado. Devolve null se não serve. */
    public static String redirecionar(String atual, String location) {
        try {
            String novo = new URI(atual).resolve(location).toString();
            return validar(novo) == null ? novo : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Falso para loopback, rede local, privada, link-local, multicast, CGNAT e endereços únicos locais do IPv6. */
    public static boolean ipPermitido(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress() || a.isMulticastAddress()) return false;
        byte[] b = a.getAddress();
        if (b.length == 4) {
            int p0 = b[0] & 0xFF, p1 = b[1] & 0xFF;
            if (p0 == 0) return false;
            if (p0 == 100 && p1 >= 64 && p1 <= 127) return false; // 100.64.0.0/10 (CGNAT)
            if (p0 == 192 && p1 == 0 && (b[2] & 0xFF) == 0) return false; // 192.0.0.0/24
            if (p0 == 198 && (p1 == 18 || p1 == 19)) return false; // testes de rede
            if (p0 >= 240) return false; // reservados e broadcast
            return true;
        }
        if (b.length == 16) {
            if ((b[0] & 0xFE) == 0xFC) return false; // fc00::/7
            return true;
        }
        return false;
    }
}
