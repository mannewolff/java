<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false displayWide=false showAnotherWayIfPresent=true>
<!DOCTYPE html>
<html lang="${(locale.currentLanguageTag)!'de'}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/logo.png" type="image/png">
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet">
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
</head>
<body class="toolbox-page ${bodyClass}">
    <main class="toolbox-shell">
        <header class="toolbox-header">
            <img src="${url.resourcesPath}/img/logo.png" alt="Toolbox" class="toolbox-logo">
            <h1 class="toolbox-title">${realm.displayName!'Mannes Toolbox'}</h1>
        </header>

        <section class="toolbox-card">
            <#-- Header der konkreten Seite (z. B. "Anmelden", "Registrieren") -->
            <#nested "header">

            <#-- Statusmeldungen aus dem Flow (Fehler, Hinweise) -->
            <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="toolbox-alert toolbox-alert-${message.type}">
                    <span class="toolbox-alert-text">${kcSanitize(message.summary)?no_esc}</span>
                </div>
            </#if>

            <#-- Eigentliches Formular der Sub-Seite (login.ftl, register.ftl, ...) -->
            <#nested "form">

            <#-- "Try another way" bei mehrstufigem Login -->
            <#if auth?has_content && auth.showTryAnotherWayLink() && showAnotherWayIfPresent>
                <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
                    <div class="toolbox-info">
                        <input type="hidden" name="tryAnotherWay" value="on"/>
                        <a href="#" id="try-another-way"
                           onclick="document.forms['kc-select-try-another-way-form'].submit();return false;">
                            ${msg("doTryAnotherWay")}
                        </a>
                    </div>
                </form>
            </#if>

            <#-- OAuth-/Social-Provider-Buttons (LinkedIn, Google etc.) -->
            <#nested "socialProviders">

            <#-- Footer-Info pro Page (z. B. "Neu hier? Registrieren") -->
            <#if displayInfo>
                <div class="toolbox-info">
                    <#nested "info">
                </div>
            </#if>
        </section>

        <#-- Branding-Block: erscheint unter der Card auf allen Login-Seiten -->
        <aside class="toolbox-branding">
            <p>
                Dieses Projekt ist vollständig privat. Es dient als Demo um zu zeigen,
                dass auch komplexere Sachen mit KI umgesetzt werden können. Es hilft mir
                meinen Prozess auszuprobieren,
                <a href="https://blog.mwolff.org/wie-ich-mit-ki-arbeite-mein-workflow-vom-gedanken-bis-zur-produktion/"
                   target="_blank" rel="noopener noreferrer">den ich hier beschrieben habe</a>.
            </p>
        </aside>
    </main>
</body>
</html>
</#macro>
