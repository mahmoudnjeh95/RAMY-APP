package com.rami.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme

// ─── Privacy Policy ───────────────────────────────────────────────────────────

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalScreen(
        title = "سياسة الخصوصية  •  Privacy Policy",
        onBack = onBack
    ) {
        LegalSection("1. جمع البيانات  —  Data Collection") {
            "نجمع المعلومات التالية عند استخدامك للتطبيق:\n" +
            "• الاسم وعنوان البريد الإلكتروني (عند التسجيل)\n" +
            "• بيانات اللعب (الإحصائيات، النتائج)\n" +
            "• معرّف الجهاز لتشغيل الإشعارات\n\n" +
            "We collect: email, username, game stats, and device ID for notifications."
        }
        LegalSection("2. استخدام البيانات  —  Data Usage") {
            "نستخدم بياناتك من أجل:\n" +
            "• تشغيل الخدمة وتحسينها\n" +
            "• عرض إعلانات ملائمة عبر Google AdMob\n" +
            "• إرسال إشعارات اللعب\n\n" +
            "Your data is used to operate the service, show relevant ads via AdMob, and send game notifications."
        }
        LegalSection("3. مشاركة البيانات  —  Data Sharing") {
            "لا نبيع بياناتك الشخصية. نشارك البيانات مع:\n" +
            "• Firebase (Google) لمصادقة الهوية والتخزين\n" +
            "• Google AdMob للإعلانات\n\n" +
            "We do not sell your data. We share with Firebase and Google AdMob only."
        }
        LegalSection("4. الأمان  —  Security") {
            "نستخدم Firebase Authentication وFirestore مع قواعد أمان صارمة لحماية بياناتك.\n\n" +
            "We use Firebase Auth and Firestore with strict security rules."
        }
        LegalSection("5. حقوقك  —  Your Rights") {
            "يمكنك في أي وقت:\n" +
            "• طلب حذف حسابك وبياناتك\n" +
            "• الاطلاع على البيانات المخزّنة\n" +
            "• إلغاء تشغيل الإشعارات من إعدادات الجهاز\n\n" +
            "You may request data deletion, access your data, or disable notifications at any time."
        }
        LegalSection("6. التواصل  —  Contact") {
            "للاستفسار: mr.njehmahmoud@gmail.com"
        }
    }
}

// ─── Terms of Service ─────────────────────────────────────────────────────────

@Composable
fun TermsScreen(onBack: () -> Unit) {
    LegalScreen(
        title = "شروط الخدمة  •  Terms of Service",
        onBack = onBack
    ) {
        LegalSection("1. قبول الشروط  —  Acceptance") {
            "باستخدامك للتطبيق، فإنك توافق على هذه الشروط. إذا كنت دون سن 13 عامًا، يجب موافقة ولي الأمر.\n\n" +
            "By using this app you agree to these terms. Users under 13 require parental consent."
        }
        LegalSection("2. الحساب  —  Account") {
            "• لا تشارك كلمة مرورك مع أحد\n" +
            "• أنت مسؤول عن نشاط حسابك\n" +
            "• نحتفظ بالحق في إيقاف حسابات تنتهك القواعد\n\n" +
            "Keep credentials private. You are responsible for your account activity."
        }
        LegalSection("3. قواعد اللعب  —  Fair Play") {
            "يُحظر صراحةً:\n" +
            "• استخدام أدوات غش أو بوتات\n" +
            "• التحرش أو الإساءة للاعبين الآخرين\n" +
            "• محاولة اختراق الخوادم\n\n" +
            "Cheating, harassment, and server exploits are strictly prohibited."
        }
        LegalSection("4. المشتريات داخل التطبيق  —  In-App Purchases") {
            "• المشتريات نهائية وغير قابلة للاسترداد إلا وفق سياسة متجر التطبيقات\n" +
            "• العملات الافتراضية (عملات وجواهر) ليست نقودًا حقيقية\n\n" +
            "Purchases are final. Virtual currency has no real-world monetary value."
        }
        LegalSection("5. الإعلانات  —  Advertising") {
            "يعرض التطبيق إعلانات عبر Google AdMob. يمكن إزالتها بالاشتراك VIP.\n\n" +
            "The app shows ads via Google AdMob. VIP membership removes ads."
        }
        LegalSection("6. تعديل الشروط  —  Changes") {
            "نحتفظ بالحق في تعديل هذه الشروط مع إشعار داخل التطبيق.\n\n" +
            "We may update these terms with in-app notice."
        }
        LegalSection("7. التواصل  —  Contact") {
            "mr.njehmahmoud@gmail.com"
        }
    }
}

// ─── Shared legal screen shell ────────────────────────────────────────────────

@Composable
private fun LegalScreen(
    title:   String,
    onBack:  () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    RamiTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF060F0A), RamiColors.DarkGreen)))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RamiColors.DarkGreen.copy(0.9f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("← رجوع", color = RamiColors.Gold) }
                Spacer(Modifier.weight(1f))
                Text(title, color = RamiColors.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = RamiColors.Gold.copy(0.25f))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun LegalSection(heading: String, body: () -> String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RamiColors.Gold.copy(0.04f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(heading, color = RamiColors.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(body(), color = RamiColors.TextLight.copy(0.7f), fontSize = 12.sp, lineHeight = 18.sp)
    }
}
