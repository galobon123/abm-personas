# Proyecto ABM de personas: de código a producción

Oct 27, 2026 · @Galo

## Objetivo

En una semana llevás un ABM de personas a producción real: servidor con dominio y HTTPS, deploy automático, logs y métricas legibles, infraestructura en Terraform y un caso de estudio listo para tu portafolio. La app es lo de menos, ya sabés hacerla; lo que aprendés es todo lo que la rodea.

El orden es deliberado: primero hacés cada cosa a mano para entender qué problema resuelve la herramienta que viene después. Servidor a mano antes que Docker, Docker antes que CI/CD, todo a mano antes de pasarlo a Terraform.

Al terminar tenés:

- La app en `https://tu-dominio` con un certificado que se renueva solo.
- Un push a `main` que corre los tests, construye la imagen y despliega sin que entres al servidor.
- Un dashboard con las cuatro señales que importan (latencia, tráfico, errores y saturación) y logs en JSON que sabés consultar.
- Un `terraform apply` que crea el servidor y un `terraform destroy` que lo borra.
- Dos sitios en el mismo servidor, un entorno de staging y un repositorio con su caso de estudio, listos para mostrar en tu portafolio.

Es una semana ajustada. Si te atrasás, lo primero que podés dejar para después es Terraform (día 5): sin él igual sabés publicar, mantener y entregar un sitio a un cliente.

## Alcance de la app

La app es un CRUD mínimo, una entidad y cinco endpoints, más cuatro piezas que los tutoriales omiten y que en producción pesan más que el CRUD.

La entidad `Persona` tiene `id`, `nombre`, `apellido`, `email` (único), `fecha_nacimiento` y `creado_en`.

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/api/personas` | Lista con paginación (`page`, `size`) |
| GET | `/api/personas/{id}` | Devuelve una persona o 404 |
| POST | `/api/personas` | Crea; 400 si el email es inválido, 409 si ya existe |
| PUT | `/api/personas/{id}` | Reemplaza los datos |
| DELETE | `/api/personas/{id}` | Borra y responde 204 |

Las cuatro piezas de producción:

- **Salud:** `/actuator/health` con liveness y readiness separadas, y `/actuator/prometheus` para métricas (Spring Boot Actuator + Micrometer).
- **Logs en JSON** a stdout, con un id de request en cada línea. Nada de archivos de log dentro del contenedor.
- **Configuración por variables de entorno** (`DB_URL`, `DB_USER`, `DB_PASSWORD`), sin secretos en el repo, y migraciones de base con Flyway.
- **Endpoints de práctica** `/debug/lento?ms=2000` y `/debug/error`, activos solo con `DEBUG_ENDPOINTS=true`. Sirven para provocar fallas en los simulacros; en un cliente real irían apagados.

El frontend es opcional: una página estática con `fetch` o un Angular de una sola vista, servida por Nginx. Si vas justo de tiempo, usá Swagger UI (springdoc) y listo.

## Stack y arquitectura

Usás tu stack habitual para la app y herramientas estándar, gratuitas y muy usadas, para todo lo demás; cada una cubre un tema de los que querés aprender.

&#91;embedded content: arquitectura de producción · 1 servidor, 3 flujos de entrada\]

Un push a `main` dispara GitHub Actions, que publica la imagen y despliega por SSH; Terraform crea el servidor, y Prometheus y Grafana vigilan la app desde adentro.

| Capa | Herramienta | Para qué |
| --- | --- | --- |
| App | Java 21 + Spring Boot, Flyway | El ABM, las migraciones y Actuator |
| Base de datos | PostgreSQL | Datos; backup con `pg_dump` |
| Contenedores | Docker + Docker Compose | Empaquetar y levantar todo junto |
| Proxy y HTTPS | Nginx (para aprender) o Caddy (HTTPS automático) + Let's Encrypt | Entrada pública, varios sitios por servidor y certificados |
| DNS y CDN | Cloudflare (plan gratuito) | DNS, caché de archivos estáticos y protección básica |
| CI/CD | GitHub Actions + GHCR | Tests, imagen y deploy |
| Métricas | Prometheus + Grafana | Dashboards y alertas |
| Logs | `journalctl`, `docker logs`, Loki (opcional) | Leer y buscar logs |
| Infraestructura | Terraform, o OpenTofu (su fork abierto, casi idéntico) | Servidor, firewall y DNS como código |

El repo tiene una carpeta por tema, así cada día toca un solo lugar:

```
abm-personas/
├── app/            # Spring Boot + Dockerfile
├── landing/        # segundo sitio estático (día 6)
├── deploy/
│   └── compose/    # docker-compose.yml, nginx, prometheus, grafana
├── infra/          # Terraform
└── .github/workflows/
```

## Antes de empezar

Necesitás un servidor Linux de 2 GB de RAM como mínimo, un dominio y unas pocas cuentas. El costo es de pocos dólares al mes y, con créditos de estudiante, puede ser cero durante el primer año.

Cuentas y herramientas:

- GitHub: repo, Actions y el registro de imágenes GHCR.
- Un proveedor de servidores con API y proveedor oficial de Terraform (opciones abajo). Todos piden tarjeta internacional o PayPal.
- Un dominio, `.com.ar` en NIC Argentina o `.com` en cualquier registrador. Podés empezar con la IP, pero HTTPS necesita dominio.
- Una cuenta gratuita de Cloudflare para el DNS y la caché del día 6.
- En tu máquina: Git, JDK 21, Docker, Terraform y un cliente SSH. En Windows usá WSL2 con Ubuntu: vas a vivir en la terminal de Linux.

Dónde correr el servidor (datos al 27/10/2026, los precios de nube cambian seguido; confirmá antes de pagar):

| Opción | Costo y condiciones | Cuándo conviene |
| --- | --- | --- |
| DigitalOcean + GitHub Student Pack | Crédito válido un año solo para cuentas nuevas verificadas como estudiantes. La página de DigitalOcean dice US$100 y la de GitHub Education US$200: confirmá el monto al reclamarlo. El droplet de US$4 tiene 512 MiB y no alcanza para la app, la base y el monitoreo; hace falta uno de 2 GB o más. | La más barata para empezar si sos estudiante regular; GitHub acepta mail institucional o constancia de inscripción, y la verificación puede tardar días. |
| Hetzner Cloud | Subió precios en abril y en junio de 2026. Un agregador de precios ubica el plan compartido de 2 vCPU y 4 GB en unos €5,49 al mes sin IVA desde el 15 de junio (aproximado). Revisá si la IPv4 se cobra aparte. | Buena relación precio/RAM si no tenés crédito. Sin datacenters en Sudamérica. |
| Oracle Cloud Always Free | Gratis sin vencimiento, pero desde junio de 2026 el tope de las instancias ARM Ampere A1 bajó a 2 OCPU y 12 GB en total. Pide tarjeta para verificar identidad y es frecuente el error de falta de capacidad. | Segunda opción: gratis, pero puede costarte horas conseguir la instancia. |

Mi recomendación: DigitalOcean con el crédito si lográs verificarte, y Hetzner si no. Los dos tienen proveedor oficial de Terraform (`digitalocean/digitalocean` y `hetznercloud/hcloud`), que usás en el día 5. Ninguno tiene datacenter en Sudamérica; para aprender, la latencia no importa.

Fuentes de los datos de costos: [DigitalOcean para estudiantes](https://www.digitalocean.com/github-students), [GitHub Student Developer Pack](https://education.github.com/pack), [resumen de cambios de precios de Hetzner, semana 25 de 2026](https://agentdeals.dev/digest/2026-w25) y [recorte del free tier de Oracle (InfoQ)](https://infoq.com/news/2026/07/oracle-cloud-free-tier-limits/).

## Plan de la semana

Cada día tiene un resultado verificable y un criterio de «listo»; si el día no lo cumple, no avances al siguiente. Calculá entre 5 y 6 horas por día y marcá las tareas a medida que las hacés.

### Día 1: la app y un servidor Linux a mano

Terminás el día con la app en un servidor con dominio y HTTPS, instalada paso a paso con tus propias manos.

- [ ] Terminar el ABM con las piezas de producción de la sección anterior y correrlo local contra Postgres (2 a 3 horas).
- [ ] Crear el servidor (Ubuntu LTS) y entrar por SSH con clave, sin contraseña.
- [ ] Endurecer lo básico: usuario propio con sudo, sin login de root ni por contraseña en `/etc/ssh/sshd_config`, firewall `ufw` con solo 22, 80 y 443, `fail2ban` y `unattended-upgrades`.
- [ ] Instalar Java 21 y PostgreSQL; crear una base y un usuario propios para la app.
- [ ] Copiar el jar con `scp` y correrlo como servicio `systemd`, con `Restart=on-failure` y un `EnvironmentFile` con permisos 600.
- [ ] Poner Nginx como proxy inverso, apuntar el dominio (registro A) y sacar el certificado con Certbot.
- [ ] Leer logs a propósito: `journalctl -u abm -f`, `/var/log/nginx/access.log`, `ss -tlnp`, `htop` y `df -h`.

**Aprendés:** SSH, usuarios y permisos, systemd, proxy inverso, DNS, TLS y dónde viven los logs en Linux.

**Listo cuando** `curl https://tu-dominio/actuator/health` responde UP desde tu casa y la app vuelve sola después de un `sudo reboot`.

Los comandos de cada paso están en [Día 1 en detalle](#mbat376t8sf.40588).

### Día 2: Docker y Compose

Terminás el día con todo el sistema levantando con un solo comando, en tu máquina y en el servidor.

- [ ] Escribir un Dockerfile multi-stage: build con JDK 21, imagen final con JRE liviano, usuario no root y `HEALTHCHECK`.
- [ ] Armar `docker-compose.yml` con la app, Postgres (con volumen nombrado) y Nginx; las variables van en un `.env` que no se sube al repo.
- [ ] Probar en tu máquina: `docker compose up -d`, `docker compose logs -f app` y `docker exec -it` a la base para mirar la tabla.
- [ ] Apagar el despliegue manual del día 1 y levantar lo mismo con Compose en el servidor.
- [ ] Programar un `pg_dump` diario con cron y restaurarlo en una base nueva para comprobar que el backup sirve.

**Aprendés:** imagen contra contenedor, capas y caché, redes y volúmenes, por qué los datos no viven dentro del contenedor y por qué un backup que no se probó no cuenta.

**Listo cuando** borrás todos los contenedores, volvés a levantar con `docker compose up -d` y los datos siguen ahí, y ya restauraste un backup en otra base.

### Día 3: CI/CD con GitHub Actions

Terminás el día con un push a `main` que despliega solo.

- [ ] Workflow de integración: en cada pull request, build y tests.
- [ ] Workflow de entrega: en `main`, construir la imagen y publicarla en GHCR con el SHA del commit como tag, nunca solo `latest`.
- [ ] Crear un usuario `deploy` en el servidor con una clave SSH exclusiva, guardada en GitHub Secrets; el workflow entra por SSH, hace `docker compose pull` y `up -d`.
- [ ] Practicar un rollback: volver al tag de la versión anterior y redesplegar.
- [ ] Verificar que ningún secreto aparezca en el repo ni en los logs del workflow.

**Aprendés:** pipelines, manejo de secretos, versionado de imágenes y despliegues reproducibles.

**Listo cuando** cambiás un texto, hacés push y en unos minutos lo ves en producción sin haber entrado al servidor, y podés volver a la versión anterior en menos de dos minutos.

### Día 4: observabilidad

Terminás el día sabiendo qué le pasa a tu app sin entrar a mirar el servidor.

- [ ] Logs: dejar la app en JSON a stdout y aprender a filtrarlos (`docker compose logs --since 10m app | grep ERROR`, `jq` sobre las líneas). Opcional: Loki con Grafana para buscarlos desde un solo lugar.
- [ ] Métricas: Prometheus que lee `/actuator/prometheus` por la red interna de Docker, sin exponerlo a internet, y Grafana con un dashboard.
- [ ] Dashboard con las cuatro señales: latencia (p95), tráfico (requests por segundo), errores (porcentaje de 5xx) y saturación (CPU, heap y conexiones del pool).
- [ ] Una alerta simple: más de X% de errores 5xx durante 5 minutos avisa por mail o Telegram desde Grafana.
- [ ] Un monitor externo de disponibilidad con plan gratuito (UptimeRobot u otro) apuntando a `/actuator/health`.
- [ ] Generar carga con k6 o `hey` mientras mirás el dashboard.
- [ ] Acceder a Grafana por túnel SSH (`ssh -L 3000:localhost:3000 usuario@IP`) o detrás de Nginx con login; nunca abierto a internet sin contraseña.

**Aprendés:** la diferencia entre logs y métricas, qué es una señal, por qué se mira el percentil 95 y no el promedio, y alertar por síntomas en lugar de por causas.

**Listo cuando**, con `/debug/lento` y `/debug/error` activos y la carga corriendo, ves el pico de latencia y de errores en Grafana, te llega la alerta y encontrás en los logs la línea responsable.

### Día 5: Terraform

Terminás el día con un servidor que existe porque un archivo lo dice.

- [ ] Instalar Terraform y crear un token de API del proveedor; va en una variable de entorno, nunca en el código ni en Git.
- [ ] Escribir en `infra/` el servidor, la clave SSH, el firewall (22, 80 y 443) y, si tu proveedor o Cloudflare gestionan el DNS, el registro A del dominio.
- [ ] Usar `cloud-init` (`user_data`) para que el servidor nazca con Docker instalado, el usuario `deploy` creado y SSH endurecido.
- [ ] Hacer `plan`, `apply`, `destroy` y `apply` otra vez, y medir cuánto tardás en tener un servidor nuevo corriendo.
- [ ] Entender qué es `terraform.tfstate`, por qué no se sube a Git y cómo guardarlo de forma remota; hacerlo si te da el tiempo.
- [ ] Probar el ciclo completo: destruir todo, aplicar, correr el workflow del día 3 y recuperar el sitio.

**Aprendés:** infraestructura declarativa, plan y apply, estado, variables, outputs y cloud-init.

**Listo cuando** `terraform destroy`, `terraform apply` y un push a `main` te devuelven la app en producción desde cero, sin hacer un solo clic en un panel.

### Día 6: varios sitios, staging y rendimiento

Terminás el día con dos sitios de dominios distintos en el mismo servidor y un entorno de prueba separado del de producción: el modelo con el que vas a atender a varios clientes.

- [ ] Sumar un segundo sitio al mismo servidor: una landing estática (HTML puro o el build de Angular) servida por Nginx con su propio server block, su dominio o subdominio y su certificado.
- [ ] Crear un entorno de staging (`staging.tu-dominio`) que se despliega solo desde una rama `develop`, mientras producción se despliega solo desde `main`.
- [ ] Poner Cloudflare (plan gratuito) delante del dominio: pasar el DNS, activar la caché de archivos estáticos y el modo de cifrado Full (strict).
- [ ] Configurar en Nginx las redirecciones de `http` a `https` y de `www` a la versión sin `www`, la compresión y la caché de assets.
- [ ] Agregar cabeceras de seguridad (`Strict-Transport-Security`, `X-Content-Type-Options` y una `Content-Security-Policy` básica).
- [ ] Medir las dos páginas con Lighthouse o PageSpeed Insights y arreglar lo que marque en rendimiento, accesibilidad y buenas prácticas.

**Aprendés:** varios sitios por servidor con server blocks, ambientes de staging y producción, qué hace un CDN, y los ajustes de rendimiento y seguridad que un cliente nota.

**Listo cuando** dos dominios responden por HTTPS desde el mismo servidor, un push a `develop` actualiza solo el staging, y Lighthouse no marca problemas de seguridad ni de HTTPS en ninguno de los dos.

### Día 7: operación, entrega y portafolio

Terminás el día con un proyecto que podés mostrar y entregar: seguro, con backups fuera del servidor y documentado.

- [ ] Auditar el servidor: `ss -tlnp` (Postgres no debe escuchar hacia internet), `sudo ufw status`, usuarios y claves SSH, y confirmar que las actualizaciones automáticas funcionan.
- [ ] Mandar el `pg_dump` diario a un lugar fuera del servidor (un bucket o el almacenamiento de otro proveedor) y restaurarlo desde cero en una base nueva.
- [ ] Escribir un runbook de una página: cómo desplegar, cómo volver atrás, cómo restaurar el backup, qué mirar si el sitio no responde y a quién avisar.
- [ ] Hacer un simulacro final: pedile a alguien que rompa algo, o rompelo vos sin mirar, y resolvelo siguiendo solo tu runbook.
- [ ] Preparar el repositorio público: README con el diagrama de arquitectura, cómo correrlo en local y cómo se despliega; capturas del dashboard de Grafana y del pipeline; y sin secretos en el historial de Git.
- [ ] Escribir un caso de estudio corto para tu portafolio: el problema, las decisiones que tomaste, qué falló en los simulacros y qué aprendiste, con el enlace a la demo en vivo.

**Aprendés:** endurecer un servidor, hacer backups que se prueban, documentar para otra persona y presentar tu trabajo.

**Listo cuando** alguien que no vio el proyecto entiende en cinco minutos qué hace y cómo está desplegado, puede abrirlo en vivo, y vos podés restaurar el sitio desde cero siguiendo solo tu runbook.

## Día 1 en detalle

El código se escribe y se compila en tu compu; el servidor solo recibe el jar ya compilado. Todo lo que tipeás en la VM son comandos de administración: instalar, configurar y mirar. Cada bloque indica dónde va.

| Qué | Dónde |
| --- | --- |
| Escribir y probar el ABM, y usar Git | Tu compu, con VS Code y Maven |
| Compilar el jar | Tu compu: `mvnw.cmd clean package` |
| Copiar el jar | De tu compu a la VM con `scp` |
| Instalar Java, Postgres y Nginx, y configurar el servicio | La VM, por SSH |
| Editar archivos de configuración del servidor | La VM, con `nano`, o desde VS Code con la extensión Remote - SSH |

```
Tu compu (VS Code, Maven, Git)             VM Ubuntu
  mvnw.cmd clean package
  scp abm.jar ───────────────────────►     /opt/abm/abm.jar (lo arranca systemd, puerto 8080)
  navegador ──── HTTPS ──────────────►     Nginx (443) ─► app (127.0.0.1:8080) ─► Postgres
```

### Paso 0: la app, en tu compu

Creá el proyecto en [start.spring.io](https://start.spring.io) o desde VS Code con la extensión Spring Initializr. Elegí Maven, Java 21 y Jar. El Artifact, por ejemplo `abm-personas`, define el nombre del jar que vas a copiar. Dependencias: Spring Web, Spring Data JPA, PostgreSQL Driver, Flyway Migration, Validation, Spring Boot Actuator y Prometheus. No marques Spring Security: bloquea todos los endpoints hasta que lo configurás.

Para desarrollar necesitás un Postgres local. Si ya tenés Docker: `docker run --name pg -e POSTGRES_USER=abm -e POSTGRES_PASSWORD=dev -e POSTGRES_DB=abm -p 5432:5432 -d postgres:16`; si no, instalalo en tu compu. Usá la misma versión mayor que tenga la VM (comprobala con psql --version en la VM): postgres:16 en Ubuntu 24.04 y postgres:18 en Ubuntu 26.04.

La configuración sale de variables de entorno, con valores por defecto para desarrollo, en `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/abm}
    username: ${DB_USER:abm}
    password: ${DB_PASSWORD:dev}
  jpa:
    hibernate:
      ddl-auto: validate
server:
  forward-headers-strategy: framework
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
  endpoint:
    health:
      probes:
        enabled: true
```

La tabla la crea Flyway con `V1__crear_persona.sql`; con `ddl-auto: validate` Hibernate solo verifica que coincida con tus entidades y no la modifica. Antes de tocar el servidor, comprobá en tu compu que el ABM responde y que `http://localhost:8080/actuator/health` devuelve `UP`.

### Paso 1: entrar a la VM por SSH con clave

En la consola de la VM averiguá la IP:

```
hostname -I
```

Desde PowerShell en tu compu (Windows trae el cliente SSH):

```
ssh tu-usuario@IP-DE-LA-VM
```

Si no conecta, revisá que la red de la VM sea de tipo puente o de solo anfitrión (ver la variante de VM local). Después creá una clave y copiala a la VM; en Windows no existe `ssh-copy-id`, así que se hace con un pipe:

```
ssh-keygen -t ed25519
type $env:USERPROFILE\.ssh\id_ed25519.pub | ssh tu-usuario@IP-DE-LA-VM "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
```

Volvé a entrar: ya no debería pedirte la contraseña de tu usuario (puede pedir la frase de la clave si le pusiste una).

### Paso 2: endurecer lo básico, en la VM

```
sudo apt update && sudo apt upgrade -y
sudo apt install -y fail2ban unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
```

Firewall. Permití SSH antes de activarlo, o te quedás afuera:

```
sudo ufw allow OpenSSH
sudo ufw allow 80,443/tcp
sudo ufw enable
sudo ufw status verbose
```

Por último, sacá el login de root y por contraseña. Hacelo solo después de comprobar que entrás con clave, y con una segunda sesión abierta por si algo falla. Usá un archivo que se lea primero (`00-`), porque sshd respeta el primer valor que encuentra y en Ubuntu otro archivo de esa carpeta puede reactivar la contraseña:

```
sudo nano /etc/ssh/sshd_config.d/00-hardening.conf
```

con este contenido:

```
PermitRootLogin no
PasswordAuthentication no
```

```
sudo sshd -t && sudo systemctl reload ssh
```

### Paso 3: Java y PostgreSQL, en la VM

```
sudo apt install -y openjdk-21-jre-headless postgresql
java -version
```

Si `apt` no encuentra Java 21, tu Ubuntu es muy viejo: usá una versión LTS reciente. Creá la base y el usuario:

```
sudo -u postgres psql
```

```
CREATE USER abm WITH PASSWORD 'una-clave-larga-y-unica';
CREATE DATABASE abm OWNER abm;
\q
```

Verificá que la base solo escuche en la propia VM: `ss -tlnp | grep 5432` debe mostrar `127.0.0.1:5432`.

### Paso 4: usuario de servicio y variables, en la VM

La app corre con un usuario sin shell y sin permisos para nada más. Sus secretos viven en un archivo fuera del repositorio:

```
sudo useradd --system --home /opt/abm --shell /usr/sbin/nologin abm
sudo mkdir -p /opt/abm && sudo chown abm:abm /opt/abm
sudo nano /etc/abm.env
```

con este contenido (la clave es la que usaste en `CREATE USER`):

```
DB_URL=jdbc:postgresql://localhost:5432/abm
DB_USER=abm
DB_PASSWORD=una-clave-larga-y-unica
SERVER_ADDRESS=127.0.0.1
```

```
sudo chmod 600 /etc/abm.env
```

`SERVER_ADDRESS=127.0.0.1` hace que la app escuche solo dentro de la VM: el único camino desde afuera será Nginx. systemd lee el archivo como root, por eso alcanza con el permiso 600.

### Paso 5: compilar y copiar el jar

En tu compu, desde la carpeta del proyecto, en PowerShell:

```
.\mvnw.cmd clean package -DskipTests
dir target\*.jar
scp target\abm-personas-0.0.1-SNAPSHOT.jar tu-usuario@IP-DE-LA-VM:/tmp/abm.jar
```

Si tenés Maven instalado, usá `mvn clean package -DskipTests`. El nombre del jar depende del Artifact y la versión de tu `pom.xml`; ignorá el archivo `.jar.original` si aparece. Saltear los tests es solo para esta prueba manual: el pipeline del día 3 los corre siempre. En la VM:

```
sudo mv /tmp/abm.jar /opt/abm/abm.jar
sudo chown abm:abm /opt/abm/abm.jar
```

### Paso 6: la app como servicio de systemd, en la VM

```
sudo nano /etc/systemd/system/abm.service
```

con este contenido:

```
[Unit]
Description=ABM de personas
After=network.target postgresql.service
Wants=postgresql.service

[Service]
User=abm
WorkingDirectory=/opt/abm
EnvironmentFile=/etc/abm.env
ExecStart=/usr/bin/java -Xmx512m -jar /opt/abm/abm.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

`-Xmx512m` limita el heap de la JVM para que no se coma toda la memoria de la VM; `Restart=on-failure` la levanta de nuevo si se cae; y `SuccessExitStatus=143` hace que systemd trate como salida normal la que tiene la JVM al recibir la señal de apagado.

```
sudo systemctl daemon-reload
sudo systemctl enable --now abm
systemctl status abm
journalctl -u abm -f
curl http://127.0.0.1:8080/actuator/health
```

El último comando debería devolver `UP`. Si la app no arranca, el motivo está en las últimas líneas de `journalctl -u abm`.

### Paso 7: Nginx como proxy inverso, en la VM

```
sudo apt install -y nginx
sudo nano /etc/nginx/sites-available/abm
```

con este contenido:

```
server {
    listen 80;
    server_name abm.local;

    location = /actuator/prometheus {
        return 404;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

El bloque de `/actuator/prometheus` evita que las métricas queden públicas: Nginx reenvía todo lo demás a la app, y ese endpoint solo tiene que verlo Prometheus por dentro de la VM. Activá el sitio:

```
sudo ln -s /etc/nginx/sites-available/abm /etc/nginx/sites-enabled/abm
sudo rm /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

Desde el navegador de tu compu probá `http://IP-DE-LA-VM/actuator/health`.

### Paso 8: HTTPS

La opción depende de si tu servidor es una VM local o uno real con dominio.

**Opción A, VM local con mkcert.** En tu compu, instalá [mkcert](https://github.com/FiloSottile/mkcert) (con winget, Chocolatey o Scoop) y generá el certificado:

```
mkcert -install
mkcert abm.local
scp abm.local.pem abm.local-key.pem tu-usuario@IP-DE-LA-VM:/tmp/
```

Editá como administrador el archivo `C:\Windows\System32\drivers\etc\hosts` y agregá una línea con la IP de la VM y el nombre: `IP-DE-LA-VM abm.local`. En la VM:

```
sudo mkdir -p /etc/ssl/abm
sudo mv /tmp/abm.local.pem /tmp/abm.local-key.pem /etc/ssl/abm/
sudo chmod 600 /etc/ssl/abm/abm.local-key.pem
```

Reemplazá el contenido de `/etc/nginx/sites-available/abm` por:

```
server {
    listen 80;
    server_name abm.local;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name abm.local;

    ssl_certificate     /etc/ssl/abm/abm.local.pem;
    ssl_certificate_key /etc/ssl/abm/abm.local-key.pem;

    location = /actuator/prometheus {
        return 404;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Recargá con `sudo nginx -t && sudo systemctl reload nginx` y abrí `https://abm.local/actuator/health`: tiene que mostrar el candado sin advertencias. Los certificados de mkcert solo los acepta una compu donde corriste `mkcert -install`, así que sirven para practicar, nunca para algo público.

**Opción B, servidor real con dominio.** El registro A del dominio tiene que apuntar a la IP pública del servidor, con los puertos 80 y 443 alcanzables, y `server_name` en Nginx con tu dominio en lugar de `abm.local`:

```
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d tu-dominio
sudo certbot renew --dry-run
```

Certbot edita la configuración de Nginx por vos; el último comando comprueba que la renovación automática funciona.

### Paso 9: leer logs a propósito y probar el reinicio

```
journalctl -u abm -f                          # logs de la app en vivo
journalctl -u abm --since '10 min ago' -p err # solo errores recientes
sudo tail -f /var/log/nginx/access.log        # cada pedido que llega
ss -tlnp                                      # qué escucha en cada puerto
htop ; df -h                                  # CPU, memoria y disco
```

En `ss -tlnp`, los puertos 22, 80 y 443 escuchan hacia afuera y los puertos 5432 y 8080 solo en `127.0.0.1`. Si ves 5432 u 8080 abiertos a todas las interfaces, volvé a los pasos 3 y 4.

Ejercicio: detené la base con `sudo systemctl stop postgresql`, pedí `/api/personas` desde el navegador, y buscá el error en `journalctl -u abm` y el código 500 en el log de Nginx. Después arrancala con `sudo systemctl start postgresql` y comprobá que la app se recupera sola. Para cerrar el día, la prueba de fuego: `sudo reboot`, esperá un minuto y verificá que `systemctl status abm` está activo y que el navegador abre la app sin que hagas nada.

### Problemas comunes del día 1

| Síntoma | Causa probable | Qué mirar |
| --- | --- | --- |
| El SSH da timeout o `refused` | Red de la VM mal configurada, o `ssh` no está corriendo | `hostname -I` en la VM y `systemctl status ssh` |
| `Permission denied (publickey)` | La clave no quedó en `authorized_keys` o los permisos son de más | `~/.ssh` en 700 y `authorized_keys` en 600 |
| 502 Bad Gateway en Nginx | La app no está corriendo | `systemctl status abm` y `journalctl -u abm` |
| `password authentication failed` al arrancar | La clave de `/etc/abm.env` no coincide con la de Postgres | Revisá el archivo, o resetéala con `ALTER USER abm WITH PASSWORD '...';` |
| Error de Flyway o de Hibernate al arrancar | La migración no coincide con las entidades | La primera línea `Caused by` de `journalctl -u abm` |
| Advertencia de certificado en el navegador | No corriste `mkcert -install`, o entrás por IP en lugar de `abm.local` | La barra de direcciones y el archivo `hosts` |
| `sshd -t` muestra un error | Un typo en el archivo de configuración | Corregilo antes de recargar; tu sesión abierta sigue funcionando |

## Variante: todo en una VM local

Todo el plan se puede hacer en una VM de tu compu sin gastar nada; solo cambian el dominio con HTTPS, el CI/CD y Terraform. Además practicás sin miedo: sacás un snapshot de la VM antes de cada simulacro (con Multipass, `multipass snapshot`; con VirtualBox, desde su interfaz) y volvés atrás si algo se rompe.

Cómo armar la VM:

- **Herramienta:** [Multipass](https://canonical.com/multipass) crea una VM Ubuntu con un solo comando. En Windows usa Hyper-V (ediciones Pro) o VirtualBox (cualquier edición). También sirven VirtualBox o VMware con la ISO de Ubuntu Server si preferís instalarla a mano, que enseña más.
- **Recursos:** 2 CPU, 4 GB de RAM y 30 GB de disco. Con 2 GB alcanza para la app y la base, pero Prometheus y Grafana del día 4 van más cómodos con 4.
- **Ejemplo con Multipass:** `multipass launch 24.04 --name abm --cpus 2 --memory 4G --disk 30G`, después `multipass shell abm` para entrar y `multipass info abm` para ver la IP. Para practicar SSH con clave, pasale tu clave pública con `--cloud-init`.
- **Red:** la VM tiene que ser alcanzable desde tu compu por IP (modo puente, o red de solo anfitrión en VirtualBox). Así abrís la app y Grafana desde tu navegador.

Qué cambia día por día:

| Día | Cambio frente al plan base |
| --- | --- |
| 1 | Mismo trabajo en la VM, pero con un nombre local en lugar de un dominio real: agregá `abm.local` al archivo `hosts` de tu compu y usá un certificado de [mkcert](https://github.com/FiloSottile/mkcert) en Nginx. Certbot y Let's Encrypt los practicás recién en un servidor real. |
| 2 | Igual. |
| 3 | Los runners de GitHub no llegan a tu VM. Instalá un [self-hosted runner](https://docs.github.com/en/actions/hosting-your-own-runners) dentro de la VM y hacé que el job de deploy corra `docker compose pull` y `up -d` ahí mismo. Usalo solo con un repositorio privado. |
| 4 | Igual; abrís Grafana desde tu navegador con la IP de la VM. |
| 5 | Sin nube no hay un servidor que crear. Practicá el ciclo `plan`, `apply`, `destroy` y el manejo del estado con el [proveedor de Docker](https://registry.terraform.io/providers/kreuzwerker/docker/latest/docs): describí la red, el volumen y el contenedor de Postgres como código. Es menos realista que crear una máquina; para eso mirá la variante de AWS. |
| 6 | Sumá los dos sitios con nombres locales en el archivo `hosts` (`abm.local` y `landing.local`) y certificados de mkcert. Cloudflare no llega a una VM local, salvo con Cloudflare Tunnel; el resto (server blocks, staging, cabeceras de seguridad y Lighthouse) se practica igual. |
| 7 | Igual. Para el backup externo, copiá el dump a tu compu o a un almacenamiento en la nube. El portafolio necesita una URL pública, así que su parte final la hacés cuando tengas un servidor real. |

Si ya tenés un dominio, [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/) permite publicar la VM sin abrir puertos en tu router; confirmá sus condiciones en su sitio.

Lo que no aprendés en una VM local: un servidor con IP pública recibe escaneos e intentos de login a los pocos minutos de existir, y el firewall del proveedor, el dominio real y los certificados de verdad solo se ven en un servidor real. Por eso conviene cerrar con la variante de AWS; además, para el portafolio necesitás una URL pública, que una VM local no te da.

## Variante: llevarlo a AWS

Sí, todo el plan se puede hacer en AWS con una instancia EC2, que es una VM en la nube, pero el plan gratuito actual es corto: para cuentas creadas desde el 15 de julio de 2025 dura hasta 6 meses o hasta que gastes los créditos, lo que ocurra primero. Los datos son del 27/10/2026; confirmalos al registrarte.

Qué te da hoy el plan gratuito:

- Al registrarte elegís plan Free o plan pago. Con el Free recibís US$100 en créditos y podés ganar hasta US$100 más completando actividades guiadas; no se te cobra nada mientras no pases al plan pago.
- El plan Free termina a los 6 meses o al gastar los créditos, y no se puede extender. Los créditos vencen a los 12 meses de crear la cuenta.
- Es solo para cuentas nuevas: si alguna vez tuviste una cuenta de AWS, no aplica. Al registrarte pide una tarjeta.

Qué instancia elegir. Las elegibles hoy para cuentas nuevas son `t3.micro`, `t3.small`, `t4g.micro`, `t4g.small`, `c7i-flex.large` y `m7i-flex.large`:

| Instancia | Memoria | Para qué |
| --- | --- | --- |
| `t3.small` | 2 GB | Todos los días. Un sitio de precios de terceros la ubica en unos US$15 al mes en Virginia del Norte. Con límites de memoria en los contenedores debería alcanzar para la app, Postgres, Nginx, Prometheus y Grafana. |
| `t3.micro` | 1 GB | Alcanza para el día 1, pero no para Compose con Prometheus y Grafana. |
| `c7i-flex.large` | Más memoria (confirmalo en la consola) | Solo si la `t3.small` se te queda corta con el monitoreo. Gasta más crédito por hora. |

Evitá las `t4g`, que son ARM: tendrías que construir las imágenes para ARM y sumás una variable que no hace falta.

Los costos que se comen el crédito:

- **IPv4 pública:** US$0,005 por hora, unos US$3,60 al mes.
- **Disco EBS:** se cobra mientras exista el volumen, aunque apagues la instancia.
- **Elastic IP:** una sin asociar se cobra igual que una en uso, así que si apagás la instancia no la dejes colgada.
- **Route 53:** US$0,50 al mes por zona de DNS, sin cobertura del free tier. Gestioná el DNS en tu registrador o en Cloudflare.
- **NAT Gateway y Load Balancer:** evitalos: cobran por hora y no los necesitás, porque Nginx dentro de la instancia reparte el tráfico entre tus sitios.

Cuenta orientativa con precios de Virginia del Norte: una `t3.small` prendida todo el mes, con su IP y un disco de 20 GB, ronda los US$20 mensuales, así que los US$100 iniciales duran unos 5 meses. Apagar la instancia cuando no estudiás estira el crédito.

Antes de crear nada:

- [ ] Elegir el plan Free al crear la cuenta, activar MFA en el usuario root y no usarlo para el trabajo diario.
- [ ] Crear un usuario IAM con permisos acotados para la CLI y Terraform; las claves van en variables de entorno, nunca en Git.
- [ ] Crear una alerta en AWS Budgets, por ejemplo un aviso al llegar a US$5 y otro a US$20.
- [ ] Anotar la fecha en que vence el plan Free.
- [ ] Elegir la región: `us-east-1` suele ser la más barata, y São Paulo (`sa-east-1`) queda más cerca de Argentina pero suele costar más (confirmalo en la calculadora de AWS). Para aprender da igual.

Qué cambia día por día:

| Día | Cambio frente al plan base |
| --- | --- |
| 1 | Lanzás la instancia desde la consola (Ubuntu LTS, `t3.small`, disco gp3 de 20 GB) y entrás con el par de claves `.pem`. El Security Group de AWS es un firewall delante de la instancia: abrí el 22 solo desde tu IP y el 80 y el 443 al mundo. Seguí usando `ufw` adentro. Asociá una Elastic IP para que la IP no cambie al reiniciar. |
| 2 | Igual. |
| 3 | GitHub Actions usa IPs variables, así que no podés limitar el puerto 22 a una sola IP. O lo abrís con login solo por clave y `fail2ban`, o instalás un self-hosted runner en la instancia. |
| 4 | Igual. CloudWatch es el equivalente de AWS y conviene conocerlo después, pero Prometheus y Grafana te enseñan más y no gastan crédito de más. |
| 5 | Terraform con el proveedor `hashicorp/aws`: `aws_instance`, `aws_security_group`, `aws_key_pair` y `aws_eip`, con la IP como output. Destruís con `terraform destroy` cuando no estudiás y recreás con `apply`. Con una cuenta real podés pasar el estado a S3 al final. |
| 6 | Los dos sitios comparten la misma instancia y la misma IP: Nginx los reparte por nombre de dominio, así que no necesitás un Load Balancer. Cloudflare va delante con su plan gratuito. |
| 7 | Copiá los backups a un bucket de S3 (para pocos MB el costo es de centavos; confirmalo en la calculadora de AWS) o al almacenamiento de otro proveedor, y dejá la demo en vivo para el portafolio mientras dure el plan. |

Cuando el plan Free termina tenés que decidir: pasar al plan pago, y los créditos que queden se aplican hasta que venzan a los 12 meses, o dejar que se cierre. Varias guías indican que al cerrarse los recursos se apagan y los datos se borran tras 90 días; guardá el código y el Terraform en Git y los backups fuera de AWS. Si querés que la demo siga en vivo en tu portafolio, pasá al plan pago o mudate a un VPS barato: con Compose y Terraform en el repo, mudarte es mucho más simple.

Cómo combinar las dos variantes: hacé los días 1, 2 y 4 en la VM local y abrí la cuenta de AWS recién cuando termines, porque los 6 meses corren desde que la creás, no desde que prendés la instancia. Reservá AWS para repetir el día 1 con dominio y HTTPS reales y para los días 3, 5, 6 y 7: la URL pública que queda funcionando es la que va a tu portafolio.

Fuentes: [FAQ del Free Tier](https://aws.amazon.com/free/faqs/), [anuncio del plan gratuito](https://aws.amazon.com/about-aws/whats-new/2025/07/aws-free-tier-credits-month-free-plan/), [instancias elegibles de EC2](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-free-tier-usage.html), [cargo por IPv4 pública](https://aws.amazon.com/blogs/aws/new-aws-public-ipv4-address-charge-public-ip-insights), [tutorial de Terraform en AWS](https://developer.hashicorp.com/terraform/tutorials/aws-get-started) y [proveedor aws de Terraform](https://registry.terraform.io/providers/hashicorp/aws/latest/docs).

## Simulacros: romperlo a propósito

A partir del día 4, rompé algo cada día y diagnosticalo sin mirar cómo lo rompiste; es la forma más rápida de aprender a operar un sistema. Usá una sola falla por vez y anotá en un archivo qué viste y cómo lo resolviste.

| Falla | Cómo provocarla | Dónde mirar | Qué aprendés |
| --- | --- | --- | --- |
| La app se queda sin memoria | Bajar el límite del contenedor a 256 MB | `docker inspect` (OOMKilled), dashboard de heap | La memoria de la JVM y el límite del contenedor son cosas distintas |
| Base de datos caída | `docker compose stop db` | Readiness en DOWN, logs de conexión, tasa de 5xx | Qué hace la app mientras no tiene base y cómo se recupera |
| Disco lleno | Crear un archivo grande con `fallocate` | `df -h`, `du -sh /var/lib/docker/*` | Rotación de logs y limpieza con `docker system prune` |
| Despliegue roto | Publicar una imagen con un bug o un tag que no existe | Pipeline, `docker compose ps`, logs de la app | Rollback rápido y por qué se versiona con SHA |
| DNS mal apuntado | Apuntar el registro A a otra IP | `dig`, `curl -v` | Distinguir DNS, TLS y aplicación |
| El certificado no renueva | `sudo certbot renew --dry-run` con el puerto 80 cerrado en el firewall | Salida de Certbot, `sudo certbot certificates`, `sudo ufw status` | Por qué un certificado puede vencerse en silencio si falla la renovación |
| Pico de latencia | `/debug/lento` más una prueba de carga | p95 en Grafana, pool de conexiones | La saturación aparece antes de que haya errores |
| Contenedor que no arranca | Quitar una variable de entorno del `.env` | `docker compose ps`, `docker compose logs` | Leer el error real en lugar de adivinar |

## Cheatsheet de comandos

Son los comandos que vas a repetir cien veces; tipealos vos en lugar de copiarlos, así quedan en la memoria.

### Linux

```
ssh -i ~/.ssh/clave usuario@IP          # entrar al servidor
journalctl -u abm -f                    # logs de un servicio systemd, en vivo
journalctl -u abm --since '1 hour ago' -p err
systemctl status abm                    # estado; también restart, enable
ss -tlnp                                # qué escucha en cada puerto
df -h ; du -sh /var/lib/docker/*        # disco
free -h ; htop                          # memoria y CPU
tail -f /var/log/nginx/access.log       # accesos en vivo
curl -I https://tu-dominio ; dig tu-dominio +short
```

### Docker

```
docker compose up -d ; docker compose ps ; docker compose down
docker compose logs -f app
docker compose pull && docker compose up -d
docker exec -it <contenedor> sh
docker stats                            # CPU y memoria por contenedor
docker inspect <contenedor>             # OOMKilled, healthcheck, montajes
docker system df ; docker system prune  # espacio usado y limpieza
```

### Nginx y certificados

```
sudo nginx -t                           # validar la configuración antes de recargar
sudo systemctl reload nginx             # aplicar cambios sin cortar conexiones
sudo certbot certificates               # certificados y fechas de vencimiento
sudo certbot renew --dry-run            # probar la renovación sin tocar nada
curl -I https://tu-dominio              # código HTTP y cabeceras de respuesta
sudo tail -f /var/log/nginx/error.log   # errores del proxy en vivo
```

### Terraform

```
terraform init ; terraform fmt ; terraform validate
terraform plan -out plan.tfplan
terraform apply plan.tfplan
terraform output ; terraform state list
terraform destroy
```

## Recursos de estudio

Leé cada recurso el día que lo usás, no antes: sirven para destrabarte mientras construís, no para estudiar de antemano. Todos son documentación oficial o material gratuito, pero no abrí cada enlace uno por uno; si alguno cambió de lugar, buscá el título en el mismo sitio.

| Día | Recurso | Para qué |
| --- | --- | --- |
| 1 | [The Linux Command Line](https://linuxcommand.org/tlcl.php) | Libro gratuito; leé permisos, procesos y redes |
| 1 | [OverTheWire: Bandit](https://overthewire.org/wargames/bandit/) | Juego por SSH para ganar soltura en la terminal; hacé los primeros niveles |
| 1 | [Certbot](https://certbot.eff.org/) | Instalar y renovar certificados de Let's Encrypt |
| 2 | [Docker: Get started](https://docs.docker.com/get-started/) | Conceptos y primer Dockerfile |
| 2 | [Spring Boot con Docker](https://spring.io/guides/topicals/spring-boot-docker/) | Imágenes en capas para apps Spring |
| 2 | [The Twelve-Factor App](https://12factor.net/) | Por qué la configuración va en variables de entorno y los logs a stdout |
| 3 | [Documentación de GitHub Actions](https://docs.github.com/en/actions) | Workflows, secretos y publicación en GHCR |
| 4 | [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html) | Health, métricas y Micrometer |
| 4 | [Prometheus: overview](https://prometheus.io/docs/introduction/overview/) | Modelo de métricas y consultas PromQL |
| 4 | [Google SRE: Monitoring Distributed Systems](https://sre.google/sre-book/monitoring-distributed-systems/) | Las cuatro señales doradas |
| 4 | [Grafana](https://grafana.com/docs/grafana/latest/) y [k6](https://grafana.com/docs/k6/latest/) | Dashboards, alertas y pruebas de carga |
| 5 | [Tutoriales de Terraform](https://developer.hashicorp.com/terraform/tutorials) | Recorrido oficial de `init` a `destroy` |
| 5 | [Provider de DigitalOcean](https://registry.terraform.io/providers/digitalocean/digitalocean/latest/docs) y [provider de Hetzner](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs) | Recursos disponibles y ejemplos |
| 6 | [Nginx: nombres de servidor](https://nginx.org/en/docs/http/server_names.html) | Varios sitios en un mismo servidor |
| 6 | [Cloudflare: documentación de DNS](https://developers.cloudflare.com/dns/) | Pasar el DNS y activar la caché |
| 6 | [Lighthouse](https://developer.chrome.com/docs/lighthouse/overview) | Medir rendimiento, accesibilidad y buenas prácticas |
| 6 | [MDN: seguridad web](https://developer.mozilla.org/en-US/docs/Web/Security) | Cabeceras de seguridad y HTTPS |
| 7 | [PostgreSQL: pg\_dump](https://www.postgresql.org/docs/current/app-pgdump.html) | Backups y restauración |
| 7 | [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/) | Buenas prácticas de seguridad para consultar |
| 7 | [Make a README](https://www.makeareadme.com/) | Estructura de un README claro para el portafolio |
| Cualquiera | [roadmap.sh/devops](https://roadmap.sh/devops) | Mapa de lo que viene después |

## Qué usar de verdad con clientes

Con clientes chicos casi nunca vas a usar Kubernetes: un servidor con Docker Compose, o un servicio gestionado, cubre la gran mayoría de los sitios. Lo que sí vas a repetir en cada proyecto son los hábitos de esta semana: HTTPS, deploy automático, backups probados y monitoreo.

| Tipo de proyecto | Qué usar | Por qué |
| --- | --- | --- |
| Landing o sitio institucional estático | Hosting estático (GitHub Pages, Cloudflare Pages, Netlify) | No hay servidor que mantener y el HTTPS viene incluido |
| App con base de datos y poco tráfico, como este ABM | Un servidor con Docker Compose, o una plataforma gestionada (Render, Railway, Fly.io) | Un solo servidor alcanza; la plataforma te ahorra operar, a cambio de un costo mayor por recurso |
| Varios clientes | Un servidor por cliente, creado con el mismo código de Terraform | Aísla fallas y costos, y es fácil de explicar y de cobrar |
| Mucho tráfico, varios servicios y alta disponibilidad | Kubernetes, preferentemente gestionado por el proveedor | Recién ahí compensa la complejidad, y conviene que lo opere un equipo |

Checklist para entregar un proyecto:

- [ ] Dominio y servidor a nombre del cliente, o con acceso documentado.
- [ ] HTTPS con renovación automática probada.
- [ ] Backups diarios de la base, guardados fuera del servidor, con una restauración probada.
- [ ] Monitoreo de disponibilidad con aviso a tu mail.
- [ ] Actualizaciones de seguridad automáticas y un día fijo para actualizar dependencias.
- [ ] Accesos y secretos en un gestor de contraseñas, nunca en el repo.
- [ ] Una página con dónde está cada cosa, cómo desplegar, cómo volver atrás y a quién llamar.
- [ ] Un acuerdo escrito de qué incluye el mantenimiento: soporte, tiempos de respuesta y costo del hosting.

## Próximos pasos

Antes de ofrecerle esto a un cliente, repetí el proceso con un sitio tuyo, como tu portfolio, y dejalo corriendo un mes: ahí aparecen los problemas reales de operar, como un certificado que no renueva, un disco que se llena o una actualización que rompe algo.

- Sumar lo que quedó afuera: logs centralizados con Loki, alertas por Telegram y pruebas de carga automáticas en el pipeline.
- Después: seguridad más a fondo (escaneo de imágenes y gestión de secretos) y lo que pida un trabajo concreto, como Kubernetes si algún día un equipo lo usa.
- Registrar cada simulacro en un archivo propio; ese cuaderno es tu manual de operaciones.
