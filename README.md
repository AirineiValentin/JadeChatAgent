# JadeChatAgent

Acest proiect implementează un sistem de chat descentralizat utilizând **JADE (Java Agent DEvelopment Framework)**. Aplicația permite agenților autonomi să comunice între ei, să descopere alți agenți activi în rețea și să păstreze un istoric al conversațiilor.

## 📝 Descrierea Problemei

În sistemele distribuite, comunicarea directă și descoperirea dinamică a participanților sunt probleme fundamentale. Această aplicație rezolvă aceste probleme prin implementarea unor agenți care:
1.  **Se auto-înregistrează** într-un serviciu de pagini aurii (Directory Facilitator - DF) pentru a fi vizibili.
2.  **Monitorizează dinamic** prezența altor agenți în sistem.
3.  **Comunică asincron** prin mesaje ACL (Agent Communication Language).
4.  **Asigură persistența datelor** prin salvarea locală a istoricului conversațiilor.

## ✨ Funcționalități Principale

* **Interfață Grafică (GUI):** Fiecare agent are propria fereastră de chat.
* **Discovery Dinamic:** Lista de destinatari se actualizează automat la fiecare 5 secunde, detectând agenții noi sau pe cei care au părăsit rețeaua.
* **Mesagerie FIPA-ACL:** Utilizarea standardului FIPA pentru schimbul de mesaje.
* **Persistență (Logging):** Istoricul conversațiilor este salvat în fișiere text (`chat_log_[nume_agent].txt`) și reîncărcat la repornirea agentului.
* **Remote Shutdown:** Funcție de "Kill Switch" care permite unui agent să trimită o comandă de oprire către toți agenții din rețea.

## 🛠️ Cerințe de Sistem

* **Java Development Kit (JDK):** Versiunea 8 sau mai nouă.
* **Biblioteci:** `jade.jar` și dependențele asociate (commons-codec, etc., incluse de obicei în distribuția JADE).

## 🚀 Instalare și Configurare

1.  **Clonarea proiectului:**
    Descarcă sursele proiectului și asigură-te că structura de pachete este respectată (`src/chatproject/ChatAgent.java`).

2.  **Configurarea IDE-ului (IntelliJ / Eclipse):**
    * Adaugă `jade.jar` în **Classpath**-ul proiectului.

3.  **Compilare:**
    Compilează fișierele `.java` din pachetul `chatproject`.

## ▶️ Lansare în Execuție

Aplicația se rulează pornind platforma JADE și inițializând agenții specifici.

**Argumente pentru Program (Run Configuration):**

Trebuie să rulezi clasa principală `jade.Boot` cu următoarele argumente:

```bash
-gui -agents agent1:chatproject.ChatAgent;agent2:chatproject.ChatAgent;agent3:chatproject.ChatAgent
