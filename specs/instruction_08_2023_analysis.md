# Instruction n° 08 du 09 Avril 2023 — Analyse & Extraction Complète
### Direction Générale du Trésor et de la Gestion Comptable des Opérations Financières de l'État

> **Source:** `Instruction-n°-08-du-09-avril-2023-FR.pdf`
> **Statut:** Document de référence légale — Ne pas modifier.
> **Rôle dans le projet:** Ce document constitue le fondement juridique de toutes les règles métier du mini-app **أمر بالإيراد**. Toute décision de conception doit être tracée vers une section de cette instruction.

---

## 1. Identification du Document

| Champ | Valeur |
|---|---|
| **Titre officiel** | Instruction n° 08 du 09 Avril 2023 |
| **Objet** | Nouveaux modèles d'Ordre de Recette |
| **Émetteur** | Direction Générale du Trésor et de la Gestion Comptable des Opérations Financières de l'État |
| **Cadre légal (1)** | Loi organique n° 18-15 du 2 septembre 2018, relative aux lois de finances (notamment article 40) |
| **Cadre légal (2)** | Loi 90-21 du 15 août 1990, relative à la comptabilité publique |
| **Cadre légal (3)** | Décret exécutif n° 46-93 du 06 février 1993 fixant les délais de paiement et de recouvrement |

---

## 2. Contexte et Objectif de l'Instruction

L'instruction a été émise dans le cadre de **l'exécution du budget programme** et du nouveau mode de gestion comptable et financière prévu par la loi organique n° 18-15.

**Objectif principal :** Fixer les nouveaux modèles d'ordres de recettes qui sont appelés à **remplacer les modèles actuels** (les anciens formulaires).

### Définition légale de l'Ordre de Recette
> *"Les ordres de recettes sont des documents par lesquels les ordonnateurs ordonnent le recouvrement de la recette publique. Ils doivent porter toutes les informations nécessaires pour permettre aux comptables publics d'effectuer le recouvrement et l'imputation budgétaire et comptable de la recette."*

**Implication pour le développement :** Chaque ordre doit contenir des informations COMPLÈTES pour le comptable. Un ordre incomplet = rejet métier.

### Mention sur le Rétablissement de Crédits
L'instruction mentionne un cas particulier : les ordres émis pour **rétablissement de crédits** (compte 212.008 — *"dépenses ordinaires du budget à annuler par suite de reversement de fonds"*). Dans ce cas, les champs **Titre** et **Catégorie/Sous-catégorie** deviennent obligatoires et doivent indiquer la source exacte des revenus.

---

## 3. Les Cinq Annexes (Modèles Officiels)

L'instruction définit **5 documents officiels** que le système doit implémenter :

### Annexe 1 — Ordre de Recette (Notifié au Comptable Public)
**Destinataire :** Le comptable public (المحاسب العمومي).
**Rôle :** Document principal par lequel l'ordonnateur autorise formellement le recouvrement de la recette.

**Champs requis :**

| # | Champ (FR) | Champ (AR) | Obligatoire | Type |
|---|---|---|---|---|
| 1 | ORDONNATEUR | الآمر بالصرف | ✅ | Texte (pré-rempli) |
| 2 | CODE ORDONNATEUR | رمز الآمر بالصرف | ✅ | Texte (pré-rempli) |
| 3 | EXERCICE | السنة المالية | ✅ | Entier (année) |
| 4 | COMPTE D'IMPUTATION | حساب القيد | ✅ | Code comptable |
| 5 | N° D'ORDRE | رقم الأمر | ✅ | Auto-généré |
| 6 | PORTEFEUILLE DE PROGRAMMES | محفظة البرامج | ✅ | Lookup |
| 7 | PROGRAMME | البرنامج | ✅ | Lookup |
| 8 | SOUS-PROGRAMME | البرنامج الفرعي | ✅ | Lookup |
| 9 | ACTION | النشاط | ✅ | Lookup |
| 10 | SOUS-ACTION | النشاط الفرعي | ✅ | Lookup |
| 11 | TITRE | العنوان | ⚠️ | TINYINT — *Uniquement pour rétablissement de crédits (1),(2)* |
| 12 | CATÉGORIE / SOUS-CATÉGORIE (le plus fin) | الصنف / الصنف الفرعي | ⚠️ | Lookup — *Uniquement pour rétablissement de crédits (1),(2)* |
| 13 | Nom du débiteur | اسم ولقب المدين | ✅ | Texte |
| 14 | Activité / Raison sociale du débiteur | النشاط / الاسم التجاري | 🟡 | Texte |
| 15 | Adresse(s) du débiteur | عنوان المدين | ✅ | Texte |
| 16 | Compte(s) courant(s) | الحساب الجاري | 🟡 | Texte |
| 17 | N° CNAS | رقم CNAS | 🟡 | Texte |
| 18 | Autres identifications (NIF, NIS…) | معلومات أخرى | 🟡 | Texte |
| 19 | MOTIF(S) | الأسباب | ✅ | Texte long |
| 20 | BASE DE LIQUIDATION | أساس التصفية | ✅ | Texte long |
| 21 | SOMME À RECOUVRER (chiffres) | المبلغ (أرقام) | ✅ | DECIMAL(15,2) |
| 22 | SOMME À RECOUVRER (lettres) | المبلغ (حروف / تفقيط) | ✅ | 🔵 Calculé — jamais saisi manuellement |
| 23 | Fait à (ville) | حرر في | ✅ | Texte |
| 24 | Le (date) | تاريخ | ✅ | Date |
| 25 | Signature Ordonnateur (cachet, chiffre, signature) | توقيع الآمر بالصرف | ✅ | Ligne de signature imprimée |

> **Texte légal narratif fixe (à configurer une seule fois) :**
> *"M/Mme ... est tenu(e) de verser à la caisse de Monsieur le Trésorier de la Wilaya de ..., qui est habilité, conformément aux dispositions du décret exécutif n° : 46-93 du 06.02.1993 d'en faire recette au compte n° : ..., Ligne (...) intitulée : ... La somme dont l'indication suit, pour les motifs ci-après énoncés."*

---

### Annexe 2 — Avis d'Émission d'Ordre de Recette (Notifié au Débiteur)
**Destinataire :** Le débiteur (المدين).
**Rôle :** Notification officielle envoyée au débiteur pour l'informer qu'un ordre de recouvrement a été émis à son encontre.

**Structure identique à l'Annexe 1** avec les mêmes champs, mais le document est physiquement **remis au débiteur** plutôt qu'au comptable.

> **Implication développement :** Les Annexes 1 et 2 partagent exactement les mêmes données source — un seul enregistrement en base de données génère **deux impressions différentes** (une pour le comptable, une pour le débiteur).

---

### Annexe 3 — Ordre d'Annulation ou de Réduction de Recette (Notifié au Comptable Public)
**Destinataire :** Le comptable public.
**Rôle :** Permet à l'ordonnateur d'annuler ou de réduire un ordre de recette déjà émis.

**Champs spécifiques à cette annexe :**

| # | Champ | Notes |
|---|---|---|
| 1 | ORDONNATEUR / CODE ORDONNATEUR | Identique à Annexe 1 |
| 2 | EXERCICE | Identique à Annexe 1 |
| 3 | PORTEFEUILLE → SOUS-ACTION | Identique à Annexe 1 |
| 4 | **N° de l'ordre de recette original** | Référence l'Annexe 1 qui est annulée/réduite |
| 5 | **Date d'émission de l'ordre original** | Date de l'Annexe 1 originale |
| 6 | **Montant original** | Montant de l'ordre à annuler/réduire |
| 7 | **Montant d'annulation/réduction** | Nouveau montant soustrait |
| 8 | **Motif de l'annulation/réduction** | Raison obligatoire |
| 9 | **Contre qui** (Mr/Mme) | Le débiteur concerné |
| 10 | Signature de l'ordonnateur | |

> **Implication développement :** Ce document implique une relation parent-enfant entre ordres. Un ordre de recette principal (Annexe 1) peut avoir **zéro ou plusieurs ordres d'annulation/réduction** (Annexe 3). La table `revenue_order_cancellation` doit être prévue en base de données.

---

### Annexe 4 — Avis d'Émission d'Ordre d'Annulation ou de Réduction de Recette (Notifié au Débiteur)
**Destinataire :** Le débiteur.
**Rôle :** Version miroir de l'Annexe 3, envoyée au débiteur pour le notifier de l'annulation ou réduction.

**Champs identiques à l'Annexe 3** + le champ supplémentaire :
- `COMPTE D'IMPUTATION` (code comptable)

> **Implication développement :** Même principe que les paires Annexes 1/2 — un seul enregistrement de cancellation génère deux impressions (comptable + débiteur).

---

### Annexe 5 — Bordereau d'Envoi des Ordres de Recette (Notifié au Comptable Public)
**Destinataire :** Le comptable public.
**Rôle :** Document de synthèse / bordereau récapitulatif qui accompagne un envoi groupé d'ordres de recettes. Sert de "bon d'expédition" pour un lot d'ordres.

**Structure du tableau récapitulatif :**

| Colonne | Description |
|---|---|
| Numéro et date des ordres de recette | Liste de tous les ordres inclus dans le bordereau |
| Noms des débiteurs | Correspond à chaque ordre |
| Montant des ordres à recouvrer | Montant de chaque ordre |
| Observations | Notes éventuelles |
| **TOTAL DES ORDRES DE RECETTES** | Sous-total du bordereau actuel |
| **TOTAL ANTÉRIEUR** | Cumul des bordereaux précédents |
| **TOTAL GÉNÉRAL** | Cumul global (TOTAL + ANTÉRIEUR) |

**En-tête du bordereau :**
- Ordonnateur / Code ordonnateur
- Exercice
- Portefeuille → Sous-action (identique aux autres annexes)
- Numéro de compte
- Numéro du bordereau (auto-généré)

> **Implication développement :** Le bordereau d'envoi est un document **regroupant plusieurs ordres**. Il faut une table `dispatch_slip` (bordereau) avec une relation N-à-N vers `revenue_order`.

---

## 4. Destinataires de l'Instruction (Qui doit l'appliquer)

### Pour exécution (obligatoire) :
- L'Agent Comptable Central du Trésor
- Le Trésorier Central
- Le Trésorier Principal

### Pour information :
- Président de la Cour des Comptes
- Chef de l'Inspection Générale des Finances
- Directeur Général du Budget
- Directeurs Régionaux du Trésor

> **Note pour la School of Magistracy :** L'institution entre dans la catégorie "ordonnateur secondaire" soumis à l'autorité du Trésorier de la Wilaya concernée. Le Secrétaire Général de l'école assume le rôle d'Ordonnateur pour les besoins de ce système.

---

## 5. Règles Métier Déduites de l'Instruction

Ces règles sont **déduites du texte légal** et doivent être implémentées dans la couche service du mini-app.

| Réf. | Règle | Source | Type |
|---|---|---|---|
| INS-01 | L'ordre de recette doit contenir TOUTES les informations nécessaires pour que le comptable public puisse effectuer le recouvrement | Art. principal | 🔴 |
| INS-02 | Le champ `SOMME À RECOUVRER (en lettres)` (التفقيط) est TOUJOURS calculé automatiquement — jamais saisi manuellement | Art. principal | 🔵 |
| INS-03 | Les champs TITRE et CATÉGORIE/SOUS-CATÉGORIE ne sont obligatoires QUE pour les opérations de rétablissement de crédits | Bas des Annexes 1/2 note (1),(2) | 🔴 |
| INS-04 | Un ordre d'annulation (Annexe 3) doit obligatoirement référencer le numéro et la date d'un ordre de recette existant (Annexe 1) | Annexe 3 | 🔴 |
| INS-05 | Un motif est obligatoire pour toute annulation ou réduction de recette | Annexe 3 | 🔴 |
| INS-06 | Le bordereau d'envoi (Annexe 5) doit calculer automatiquement le TOTAL ANTÉRIEUR (cumul des bordereaux précédents) et le TOTAL GÉNÉRAL | Annexe 5 | 🔵 |
| INS-07 | La référence légale exacte du décret `n° 46-93 du 06.02.1993` doit apparaître dans le texte narratif des Annexes 1 et 2 | Annexes 1/2 | 🔴 |
| INS-08 | Deux documents sont produits pour chaque événement : un pour le comptable (Annexe 1 ou 3), un pour le débiteur (Annexe 2 ou 4) | Structure de l'instruction | 🔵 |

---

## 6. Correspondance Annexes → Écrans du Mini-org.marrok.amriirad.App

| Annexe officielle | Écran correspondant | Action utilisateur |
|---|---|---|
| Annexe 1 | Formulaire Ordre de Recette | Créer / Imprimer (version comptable) |
| Annexe 2 | Formulaire Ordre de Recette | Imprimer (version débiteur) — même données |
| Annexe 3 | Formulaire Annulation/Réduction | Créer un ordre d'annulation lié |
| Annexe 4 | Formulaire Annulation/Réduction | Imprimer (version débiteur) — même données |
| Annexe 5 | Écran Bordereau d'Envoi | Grouper des ordres + imprimer le bordereau |

---

## 7. Impact sur le Schéma de Base de Données

Par rapport au schéma initial prévu dans `amr_iirad_miniapp_plan.md`, l'analyse de l'Instruction 08 impose les ajouts suivants :

### Table supplémentaire : `revenue_order_cancellation` (Annexes 3 & 4)
```sql
CREATE TABLE revenue_order_cancellation (
    id                      BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    -- Référence à l'ordre original
    original_order_id       BIGINT UNSIGNED     NOT NULL,           -- FK -> revenue_order.id
    -- Budgetary imputation (identique à revenue_order)
    fiscal_year             SMALLINT UNSIGNED   NOT NULL,
    -- Annulation
    cancellation_type       ENUM('ANNULATION','REDUCTION') NOT NULL,
    cancelled_amount        DECIMAL(15,2)       NOT NULL,           -- Montant annulé/réduit
    reason                  TEXT                NOT NULL,           -- Motif obligatoire
    -- Status
    status                  ENUM('DRAFT','SUBMITTED','APPROVED') NOT NULL DEFAULT 'DRAFT',
    -- Authorization
    issued_at_city          VARCHAR(100)        DEFAULT NULL,
    issued_date             DATE                DEFAULT NULL,
    -- Audit
    created_at              DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(50)         DEFAULT NULL,
    is_deleted              TINYINT(1)          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (original_order_id) REFERENCES revenue_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Cancellation/Reduction orders — Annexes 3 and 4 of Instruction 08';
```

### Table supplémentaire : `dispatch_slip` (Bordereau — Annexe 5)
```sql
CREATE TABLE dispatch_slip (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    slip_number     VARCHAR(20)         NOT NULL,           -- Numéro du bordereau (auto-généré)
    fiscal_year     SMALLINT UNSIGNED   NOT NULL,
    account_number  VARCHAR(50)         DEFAULT NULL,       -- Numéro de compte
    total_amount    DECIMAL(15,2)       NOT NULL DEFAULT 0, -- TOTAL DES ORDRES (calculé)
    previous_total  DECIMAL(15,2)       NOT NULL DEFAULT 0, -- TOTAL ANTÉRIEUR
    general_total   DECIMAL(15,2)       NOT NULL DEFAULT 0, -- TOTAL GÉNÉRAL (calculé)
    issued_date     DATE                DEFAULT NULL,
    -- Audit
    created_at      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)         DEFAULT NULL,
    is_deleted      TINYINT(1)          NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Dispatch slip (Bordereau d envoi) — Annexe 5 of Instruction 08';

-- Table de liaison : un bordereau contient plusieurs ordres
CREATE TABLE dispatch_slip_order (
    slip_id         BIGINT UNSIGNED NOT NULL,
    order_id        BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (slip_id, order_id),
    FOREIGN KEY (slip_id)  REFERENCES dispatch_slip(id),
    FOREIGN KEY (order_id) REFERENCES revenue_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 8. Texte Légal Narratif Fixe (à stocker en configuration)

Le texte suivant apparaît dans les Annexes 1 et 2 et doit être **stocké en configuration** (non modifiable par l'utilisateur standard) avec deux variables dynamiques :

```
M/Mme [DEBTOR_NAME] est tenu(e) de verser à la caisse de Monsieur le Trésorier
de la Wilaya de [WILAYA_NAME], qui est habilité, conformément aux dispositions
du décret exécutif n° : 46-93 du 06.02.1993 d'en faire recette au compte n° :
[ACCOUNT_NUMBER], Ligne ([LINE_NUMBER]) intitulée : [ACCOUNT_TITLE].
La somme dont l'indication suit, pour les motifs ci-après énoncés.
```

**Variables de configuration (fixées une fois par dossier) :**
- `WILAYA_NAME` : ex. "Alger"
- `ACCOUNT_NUMBER` : ex. "1980000034/55" (RIB du Trésor)
- `LINE_NUMBER` : numéro de la ligne budgétaire
- `ACCOUNT_TITLE` : intitulé du compte

---

*Ce document est une extraction et analyse fidèle de l'Instruction n° 08 du 09 avril 2023.*
*Il sert de référence légale permanente pour le projet amr-iirad.*
*Toute règle métier doit être tracée vers une règle INS-xx de ce document.*
