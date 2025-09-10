# Icônes MQFTPServer 🎨

Ce document décrit les icônes créées pour l'application MQFTPServer et leur utilisation.

## 🎯 Design Concept

L'icône de MQFTPServer combine plusieurs éléments visuels pour représenter l'application :

### **Éléments Visuels**
- **🥽 Casque VR** (rouge) - Représente la compatibilité Meta Quest
- **🏗️ Tour de serveur** (bleu turquoise) - Symbolise le serveur FTP
- **⇄ Flèches de transfert** (jaune) - Indiquent les transferts de fichiers
- **🔴 Indicateurs LED** - Montrent l'activité du serveur
- **📡 Signaux WiFi** - Représentent la connectivité réseau

### **Palette de Couleurs**
- **Fond principal** : `#16213E` (bleu marine foncé)
- **Fond secondaire** : `#0F3460` (bleu foncé)
- **Casque VR** : `#FF6B6B` (rouge corail)
- **Serveur** : `#4ECDC4` (turquoise)
- **Transferts** : `#FFE66D` (jaune doré)

## 📱 Types d'Icônes Créées

### **1. Icônes Adaptatives (Android 8.0+)**
- `ic_launcher_background.xml` - Arrière-plan avec dégradé
- `ic_launcher_foreground.xml` - Éléments de premier plan
- `ic_launcher.xml` - Configuration de l'icône adaptative
- `ic_launcher_round.xml` - Version ronde de l'icône adaptative

### **2. Icônes Traditionnelles (Multi-DPI)**
Créées pour toutes les densités d'écran :
- **MDPI** (48x48dp) - `mipmap-mdpi/`
- **HDPI** (72x72dp) - `mipmap-hdpi/`
- **XHDPI** (96x96dp) - `mipmap-xhdpi/`
- **XXHDPI** (144x144dp) - `mipmap-xxhdpi/`
- **XXXHDPI** (192x192dp) - `mipmap-xxxhdpi/`

### **3. Icônes Spécialisées**
- `ic_notification.xml` - Icône de notification (24x24dp)
- `ic_feature_graphic.xml` - Graphique de fonctionnalité pour store (512x512dp)

## 🛠️ Fichiers Modifiés

### **AndroidManifest.xml**
```xml
<application 
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:theme="@style/Theme.Material3.Dark">
```

### **Structure des Dossiers**
```
app/src/main/res/
├── drawable/
│   ├── ic_launcher_background.xml
│   ├── ic_launcher_foreground.xml
│   ├── ic_notification.xml
│   └── ic_feature_graphic.xml
├── mipmap-anydpi-v26/
│   ├── ic_launcher.xml
│   └── ic_launcher_round.xml
├── mipmap-mdpi/
│   ├── ic_launcher.xml
│   └── ic_launcher_round.xml
├── mipmap-hdpi/
│   ├── ic_launcher.xml
│   └── ic_launcher_round.xml
├── mipmap-xhdpi/
│   ├── ic_launcher.xml
│   └── ic_launcher_round.xml
├── mipmap-xxhdpi/
│   ├── ic_launcher.xml
│   └── ic_launcher_round.xml
└── mipmap-xxxhdpi/
    ├── ic_launcher.xml
    └── ic_launcher_round.xml
```

## 🎨 Utilisation des Icônes

### **Launcher d'Application**
- **Android 8.0+** : Utilise l'icône adaptative avec foreground/background
- **Android < 8.0** : Utilise les icônes statiques dans mipmap-*/
- **Launchers ronds** : Utilise automatiquement `ic_launcher_round`

### **Notifications**
```kotlin
// Utilisation dans le code Kotlin
.setSmallIcon(R.drawable.ic_notification)
```

### **Store Meta Quest**
- Utiliser `ic_feature_graphic.xml` exporté en PNG 512x512
- Optimisé pour l'affichage dans le Meta Store

## 🔧 Personnalisation

### **Modifier les Couleurs**
Pour changer la palette de couleurs, modifier les valeurs dans :
- `ic_launcher_background.xml` - Dégradé d'arrière-plan
- `ic_launcher_foreground.xml` - Couleurs des éléments

### **Adapter le Design**
- **Casque VR** : Modifier la forme dans `android:pathData`
- **Serveur** : Ajuster la taille et les segments
- **Flèches** : Changer la direction ou le style

## 📐 Spécifications Techniques

### **Icône Adaptative**
- **Taille totale** : 108x108dp
- **Zone sûre** : 66x66dp (centré)
- **Format** : Vector Drawable (XML)

### **Icônes Traditionnelles**
- **Format** : Vector Drawable (XML) évolutif
- **Compatibilité** : Android 5.0+ (API 21+)
- **Performance** : Optimisé pour le rendu GPU

### **Accessibilité**
- **Contraste** : Respect des standards WCAG
- **Visibilité** : Lisible sur fonds clairs et foncés
- **Taille** : Respect des guidelines Android pour les tailles minimales

## 🚀 Déploiement

### **Build APK**
Les icônes sont automatiquement incluses lors du build :
```powershell
.\build-release-simple.ps1
```

### **Meta Store**
1. Exporter `ic_feature_graphic.xml` en PNG 512x512
2. Utiliser pour la carte d'application dans le Meta Store
3. L'icône launcher sera utilisée pour l'icône d'application

## 💡 Conseils de Design

### **Cohérence Visuelle**
- Maintenir les proportions entre les éléments
- Respecter la palette de couleurs définie
- Assurer la lisibilité à toutes les tailles

### **Évolutivité**
- Utiliser des Vector Drawables pour une qualité parfaite
- Prévoir les futures versions d'Android
- Maintenir la compatibilité avec les anciens appareils

### **Branding**
- L'icône reflète l'identité VR + FTP de l'application
- Reconnaissable dans le contexte Meta Quest
- Distinctif parmi les autres applications serveur

---

**Les icônes MQFTPServer sont maintenant prêtes pour le déploiement et l'affichage dans les stores !** 🎉
