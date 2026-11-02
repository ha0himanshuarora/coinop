package com.coinop.budget.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** The 11-colour theme model of the Arcadia design language. */
data class ThemeColors(
    val bg: Color,
    val surface: Color,
    val variant: Color,
    val onVariant: Color,
    val outline: Color,
    val ink: Color,
    val cream: Color,
    val red: Color,
    val green: Color,
    val mustard: Color,
    val floor: Color,
)

class ThemeDef(val id: String, val name: String, val dark: ThemeColors, val light: ThemeColors)

private fun colors(spec: String): ThemeColors {
    val c = spec.trim().split(" ").map { Color(("FF$it").toLong(16)) }
    require(c.size == 11) { "theme needs 11 colours" }
    return ThemeColors(c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9], c[10])
}

// colour order: bg surface variant onVariant outline ink cream red green mustard floor
private fun def(id: String, name: String, dark: String, light: String) =
    ThemeDef(id, name, colors(dark), colors(light))

object Themes {
    val all: List<ThemeDef> = listOf(
        def("arcadia", "Arcadia",
            "1F1D1B 2B2825 38342F B5A688 4A4540 1C1B1A DCC9A9 B83A2D 4E6851 E8A04A 223027",
            "DCC9A9 F3E8CF CDB88F 4A4237 1C1B1A 1C1B1A DCC9A9 B83A2D 4E6851 E8A04A 223027"),
        def("gameboy", "Game Boy",
            "0B2A0B 0F380F 1B4D1B 8BAC0F 306230 0B2A0B 9BBC0F 306230 8BAC0F 9BBC0F 0F380F",
            "CADC9F E0EBC0 B5C98A 306230 0F380F 0F380F CADC9F 306230 8BAC0F 9BBC0F 0F380F"),
        def("synthwave", "Synthwave",
            "140A24 1F1237 2D1B4D B79BE0 4B2D7A 0E0618 F5E1FF FF2E97 00C2D1 FFD23F 1E1040",
            "F1DDFB FBF0FF E0C3F3 5A3B7A 1A0B2E 1A0B2E F5E1FF FF2E97 00A9BC FFD23F 1E1040"),
        def("phosphor", "Phosphor",
            "04100A 081C10 0F2C19 4FD16F 1F6B33 020A05 7CFF9A 1F8A3B 0E5A25 39FF6A 081C10",
            "CFE8CF E6F6E6 B7D8B7 1F5A2A 0B2B12 0B2B12 CFE8CF 1F8A3B 0E5A25 6BFF8A 0B2B12"),
        def("amber", "Amber CRT",
            "140C00 211500 332000 E0A030 6B4A12 0A0600 FFC94D C25E00 7A5200 FFB000 1A1000",
            "F0DDB5 F8EBCB E2C88F 6B4A12 2A1800 2A1800 F0DDB5 C25E00 7A5200 FFB000 2A1800"),
        def("candy", "Candy",
            "2A1230 3A1B42 4D2757 E0A8CC 7A3B8A 1A0820 FFD6EA E8457E 4FB8A4 FFC93C 3A1B42",
            "F8D5E4 FFEEF5 EDB8D0 7A3555 3B1030 3B1030 F8D5E4 E8457E 3F9C8B FFC93C 4A2150"),
        def("miami", "Miami",
            "07262B 0C3840 134B55 8FD6D0 1F7A80 041A1E CFFAF4 FF6F61 14A3A0 FFD166 0B3A40",
            "CFEFEA E8FAF6 ABD9D2 1F5A58 0B2E33 0B2E33 CFEFEA E8574A 0E8A87 FFD166 0B3A40"),
        def("superpad", "Super Pad",
            "0F1535 18214F 253270 9FB0E8 3B4C9E 080C22 E3E8FA D62839 2B4BB3 F5C400 18214F",
            "D5DCEF EEF1FA B9C4E3 2F3F73 10183A 10183A D5DCEF D62839 2B4BB3 F5C400 14214F"),
        def("papermono", "Paper Mono",
            "141414 1F1F1F 2C2C2C A8A8A0 5A5A56 0A0A0A EDEDE6 4A4A46 8A8A84 EDEDE6 1F1F1F",
            "E6E6E1 F7F7F3 D0D0CA 4A4A46 111111 111111 E6E6E1 2B2B2B 6B6B66 BDBDB5 1E1E1C"),
        def("sunset", "Sunset Strip",
            "24101C 361A2B 4A2539 E6A98E 8A4A5C 14060E FFE2CC D9482B 7B3FA0 FFB23F 361A2B",
            "F6D9C4 FFEEE0 E9BFA3 7A3A2A 3A1420 3A1420 F6D9C4 D9482B 7B3FA0 FFB23F 4A1F3D"),
        def("virtualboy", "Virtual Boy",
            "120202 1E0505 330A0A E86A5A 7A1414 080000 FF8A7A E00A0A 8A1414 FF3A2A 1E0505",
            "F2D5D0 FFE8E4 E4B3AB 7A1F1F 2A0505 2A0505 F2D5D0 E00A0A 8A1414 FF5A4A 2A0505"),
        def("neonnight", "Neon Night",
            "0D0D1A 17172B 24244A A8A8E0 4A4A8A 05050D FFF6A0 FF2A6D 05D9E8 FFE600 17172B",
            "F5F0B8 FFFBD0 E2DC8A 4A4600 14120A 14120A F5F0B8 FF2A6D 05A8C0 FFE600 1B1B2F"),
        def("cocoa", "Cocoa",
            "1E140C 2C1E13 3E2B1C C9A57E 6B4A2F 120A05 EBD5B5 C0561F 6B7A42 F0A93B 2C1E13",
            "E5D3BC F3E7D6 D0B697 5A3A22 2B1A0E 2B1A0E E5D3BC A8481E 5E6B3A F0A93B 3A2416"),
        def("ice", "Ice",
            "08141F 0F2133 183552 8FBCE0 2F5F8A 040B12 DCEFFC 3A8EE0 22B8A6 F2C94C 0F2133",
            "D4E6F2 EAF4FA B5D0E3 24506E 0B2438 0B2438 D4E6F2 2B7CC4 1E9E8E F2C94C 12324D"),
        def("tokyo", "Tokyo Drift",
            "14112B 1F1A40 2D2759 B0A8E8 4F4690 090718 EDE8FF F0558A 4FC3DC F5C453 1F1A40",
            "E6E2F5 F4F1FD CFC8EA 4A3F7A 1A1533 1A1533 E6E2F5 E0457B 3FA0B8 F2B84B 1F1B3D"),
        def("mintchoc", "Mint Choc",
            "0A1C14 112A1E 1B3D2D 8CD4B0 2F6B4F 040E09 DDF7EA EE6A8A 3AA880 F2D16B 112A1E",
            "D5EFE2 EAF8F0 B4DEC8 27604A 12301F 12301F D5EFE2 E0587A 2E8B6A F2D16B 1C3D2D"),
        def("lava", "Lava",
            "140704 211009 361A0E E89A6A 7A3A1A 080301 FFC9A0 FF4A12 A8461A FF9A1F 211009",
            "F4D9C2 FFEBDC E8BA97 7A2E10 2B0F05 2B0F05 F4D9C2 E03A10 8A3A12 FF9A1F 2B0F05"),
        def("forest", "Forest",
            "0A140A 122212 1D361C 9CC68E 36643A 050A05 DDEFCB C9602F 4F9A48 E8B84A 122212",
            "D6E3C8 EBF3E0 B9CFA5 2F5530 15260F 15260F D6E3C8 B5502B 3E7A38 E8B84A 1F3A1A"),
        def("bubblegum", "Bubblegum",
            "2B0F26 3D1836 55264C F0A0D2 8A3A7A 160512 FFD9EE FF5FAE 6AD2F0 FFE45C 3D1836",
            "FBD3E8 FFEAF5 F2A9CF 8A2D66 4A0F36 4A0F36 FBD3E8 FF4FA3 5CC8E8 FFE45C 5A2352"),
        def("sandstorm", "Sandstorm",
            "1C1608 2A210E 3C3016 D2B878 6E5A28 0E0A03 F2E2B8 D2622E 7C9C44 F0B429 2A210E",
            "EFE0BE F8EED6 DCC592 6B5320 2E2208 2E2208 EFE0BE C2562A 6B8A3A F0B429 3E2F10"),
        def("midnight", "Midnight",
            "060A1E 0D1432 17214A 90A0E0 2E3E8A 03050F E0E6FF 4A6FE0 3AA0B0 F2C94C 0D1432",
            "D8DEEE ECEFF8 BAC4E0 2A3566 0A0F2A 0A0F2A D8DEEE 3355C8 2A8A9A F2C94C 0F1838"),
        def("cherrycola", "Cherry Cola",
            "1A0508 2A0A10 400F18 E89098 7A1F2C 0A0204 FFD5D5 E0203A 8A4A3A F2B33D 2A0A10",
            "F0CFCF FAE6E6 E0A5A5 7A1A22 2E0508 2E0508 F0CFCF C4152B 6E3A2E F2B33D 3B0A10"),
        def("slate", "Slate",
            "12161B 1B2128 27303A 9FB0C0 3D4A57 0A0D10 E4ECF2 E5703A 4FA3A5 F0C05A 1B2128",
            "D9E1E8 EEF3F7 BFCCD7 3A4A58 0D1319 0D1319 D9E1E8 D9582A 2F8587 F0C05A 16202A"),
        def("citrus", "Citrus",
            "14180A 1F2610 2F3A18 CBDB7A 55662A 0A0D04 F3F7CF FF7A1A 9ACD32 FFD83A 1F2610",
            "EAF0C4 F7FAE0 D6E19A 4A5A12 1B2108 1B2108 EAF0C4 E86410 6E9E1A FFD83A 232C0C"),
        def("cyberpunk", "Cyberpunk",
            "08080D 111119 1B1B27 8C8CAE 00D9E6 050508 EEF0FA FF2A6D 00F0FF FCEE0A 08080D",
            "F1EDD0 FFFBE0 E0DAAA 4A4630 0A0A10 0A0A10 F1EDD0 E0145A 00A5B5 FCEE0A 0A0A10"),
    )

    fun byId(id: String): ThemeDef = all.firstOrNull { it.id == id } ?: all.first()
}

/** Main accent: mustard pops on dark faces, red leads on light ones. */
fun ThemeColors.primary(dark: Boolean): Color = if (dark) mustard else red

/** Accent for the opposite role (used for debits/warnings). */
fun ThemeColors.tertiary(dark: Boolean): Color = if (dark) red else mustard

private fun contrast(a: Color, b: Color): Float {
    val l1 = a.luminance() + 0.05f
    val l2 = b.luminance() + 0.05f
    return if (l1 > l2) l1 / l2 else l2 / l1
}

/** Text/icon colour for a coloured face: ink or cream, whichever contrasts better. */
fun ThemeColors.onFace(face: Color): Color =
    if (contrast(face, ink) >= contrast(face, cream)) ink else cream
