package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun MedicalSafetyProtocolDialog(
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = SpaceSurfaceCard,
      border = BorderStroke(2.dp, StatusSevereRed),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.MedicalServices,
            contentDescription = null,
            tint = StatusSevereRed,
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "PROTOCOLO DE SEGURIDAD MÉDICA",
              style = MaterialTheme.typography.titleMedium,
              color = StatusSevereRed,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Directriz de Medicina Aeroespacial (NASA / ESA)",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(StatusSevereRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Text(
            text = "REGLA CARDINAL DE VUELO ESPACIAL:\nNunca atribuir dolor torácico, disnea aguda severa, mareo sincopal o arritmias marcadas a ansiedad o causas psicológicas sin descarte médico previo exhaustivo.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Signos de Alarma Física (Red Flags) que requieren contacto inmediato con el Cirujano de Vuelo (Flight Surgeon):",
          style = MaterialTheme.typography.bodySmall,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val redFlags = listOf(
          "Dolor o pesadez torácica retroesternal irradiada a mandíbula o extremidad superior.",
          "Dificultad respiratoria súbita (disnea intensa) no atribuible a ejercicio ni a CO2 elevado.",
          "Episodios de presíncope, desmayo o desorientación neurológica aguda.",
          "Lecturas ambientales anómalas: CO2 > 0.8% o saturación O2 < 93% en el módulo.",
          "Frecuencia cardíaca en reposo sostenida > 120 lpm o arritmias hemodinámicamente inestables."
        )

        redFlags.forEach { flag ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = StatusModerateOrange,
              modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = flag,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Diferenciación Operativa:\nSPACE MIND monitorea el bienestar psicológico y ofrece asistencia para la regulación del estrés. No diagnostica ni sustituye la atención médica clínica de la tripulación.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = SpaceDeepNavy),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = "ENTENDIDO Y CONFORME",
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
fun PrivacyProtocolDialog(
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = SpaceSurfaceCard,
      border = BorderStroke(1.dp, VioletNeon),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = VioletSoft,
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "PRIVACIDAD Y ÉTICA CLÍNICA",
              style = MaterialTheme.typography.titleMedium,
              color = VioletSoft,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Protección de Datos Psicológicos del Astronauta",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val privacyPoints = listOf(
          "Confidencialidad Médica: Los registros de ansiedad y voz pertenecen exclusivamente al astronauta y su psicólogo clínico.",
          "Micrófono Bajo Demanda: El micrófono se activa solo cuando lo pulsas; nunca permanece abierto ni almacena grabaciones de audio.",
          "Enlace IA Seguro: Las transcripciones temporales se procesan mediante Firebase AI Logic con respaldo local sin conexión.",
          "Sin Divulgación a Mando: Ningún registro psicológico es accesible por comandancia sin consentimiento informado.",
          "Control Total: Puedes pausar, repetir, corregir o purgar tus sesiones en cualquier momento."
        )

        privacyPoints.forEach { point ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = TealLight,
              modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = point,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = VioletNeon, contentColor = Color.White),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = "CONFIRMAR PRIVACIDAD",
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
