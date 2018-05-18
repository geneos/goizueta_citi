## Introducción

Plugin de Libertya ERP 15.03 para generar archivos TXT según RG 3685 de AFIP (Régimen de información compras y ventas).

Para poder generar los archivos, el sistema deberá poder relacionar diferentes datos del sistema con códigos provistos por la AFIP. Estos datos son:

* **Tipo de Comprobante**

    Para este fin se agregó un campo desplegable en las ventanas de *Facturas Cliente* y *Facturas Proveedor*. Este plugin intenta completar este campo automáticamente, de acuerdo a como se haya configurado el *Tipo de Comprobante* en cuestión.

* **Tipo de Impuesto**

    Para poder relacionar este dato, este plugin agrega un campo desplegable en la ventana `Categoría de Impuestos`, pestaña `Impuesto`.

* **Moneda**

    Se utiliza el campo c_currency.wsfecode ya existente en Libertya ERP. Este campo puede *no* estar visible por default en la ventana de `Moneda`, dentro del perfil `Configuración de la Compañía`.

## Instalación

### Instalar Plugin

1. Copiar el archivo *jar* hacia `$OXP_HOME/lib/plugins/`
2. Ingresar a Libertya ERP con usuario System e instalar el plugin utilizando el *Instalador de Componentes*
3. Si se está utilizando la interfaz web, reiniciar el servicio de JBOSS

### Configurar Tipos de Comprobantes

#### Tipos de Comprobantes AFIP

Antes que todo, asegurarse que la referencia `DocSubTypeCae` tiene declarados todos los códigos de comprobantes con los que se desea trabajar. Para más información ver el [listado](http://www.afip.gov.ar/efactura/documentos/TABLA%20TIPO%20COMPROBANTES%20V.0%20%2025082010.xls) de la AFIP. IMPORTANTE: Los códigos de comprobantes deben ser cargados con dos dígitos máximo (e.g.: 001 -> 01)

#### Tipos de Comprobantes Libertya ERP

Si se quiere asociar de forma predeterminada un TdC de Libertya ERP con un TdC de la AFIP, esto podrá hacerse desde la ventana `Tipos de Documento` dentro del perfil `Configuración de la Compañía`. Allí, el TdC de la AFIP figura por default como `Tipo de Documento Electrónico`. Tener en cuenta que este campo puede o no estar visible, de acuerdo a la configuración propia de cada TdC.

### Configurar Impuestos

Ingresar a la ventana `Categoría de Impuestos`, luego en la pestaña `Impuesto`, para cada registro activo y que no sea del tipo _Carpeta_ deberá configurarse

#### Código de impuesto correspondiente a esta RG.

A modo de ayuda, se puede ejecutar el siguiente script en la base de datos

```
update c_tax set citirg3685 = 
	case 
		when rate = 0 then 'EXE' 
		else 'CDF' 
	end
where 
	citirg3685 is null 
	and isactive = 'Y'
	and rate in (0, 21, 10.5)
;
```

####  Alícuota de IVA / Operación / Condición IVA / WSFE

Parece que no, pero estamos hablando de un solo campo, referido de diferentes formas tanto por la AFIP como por el sistema. Para mayor información, consultar las tablas de valores de la AFIP.

A modo de ayuda, se puede ejecutar el siguiente script en la base de datos

```
update c_tax set wsfecode = 
	case 
		when istaxexempt = 'Y' then 2
		when rate = 0 then 3
		when rate = 10.5 then 4
		when rate = 21 then 5
		when rate = 27 then 6
		when rate = 5 then 8
		when rate = 2.5 then 9
		else wsfecode 
	end
where 
	wsfecode is null 
	and isactive = 'Y' and issummary = 'N'
	and (rate in (0, 2.5, 5, 10.5, 21, 27) or istaxexempt = 'Y')
;
```

## Utilización

Para la determinación del tipo de comprobante se debe especificar, al momento de cargar el documento, qué tipo de comprobante se trata según el listado que establece AFIP. El campo para
hacerlo está debajo de "Tipo de documento". Esto se debe hacer solamente para comprobantes de compras; en los de ventas se determinan según lo configurado en la ventana "Tipo de documento",
perfil "Configuración de la compañía".

Este campo está configurado para poder modificarse aún después de completada la factura.

## Detalles

La versión base de Libertya ERP en la que se hizo el desarrollo es 15.03. Está implementado como un plugin que extiende las clases c_invoice y c_tax. Agrega, además, una columna a cada tabla.

Los comprobantes anteriores a la fecha de instalación del plugin no se actualizan (ver *Anexos*).

Los archivos install.xml, postinstall.xml y preinstal.sql contienen las modificaciones a las tablas y diccionario de datos necesarios, tal como los genera el exportador de de plugins 
de Libertya ERP.

## Créditos

### Juan Manuel Martínez
Comentarios, sugerencias, dudas y demás por correo a jmmartinezsf@gmail.com

### Saulo José Gil 

Contacto vía [website de Orbital Software Argentina](http://www.orbital.com.ar)

## Anexos

### Utilización retroactiva

A la hora de exportar los archivos CITI el plugin utiliza un campo que no es completado retroactivamente (ver *Detalles*). Para facilitar este proceso, puede adaptarse y ejecutarse el siguiente script SQL:

```
begin work;
update c_invoice as i set 
	afipdoctype = case
		-- Otros comprobantes
		when dt.doctypekey in (
				'RTI'	-- Retencion_Invoice
				, 'RTR' -- Retencion_Receipt
				, 'RCI' -- Retencion_InvoiceCustomer
				, 'RCR' -- Retencion_ReceiptCustomer
			) then '99' -- Otros Comprobantes
		
		-- Facturas Cliente
		when 
			i.issotrx = 'Y' 
			and dt.docsubtypecae is not null 
			then dt.docsubtypecae -- Toma docsubtypecae como referencia

		-- Facturas Proveedores
		when 
			dt.docbasetype = 'API' and i.c_letra_comprobante_id in(1010039, 1010040) then
			case
				when i.c_letra_comprobante_id = 1010039 then '01' -- A
				else '06' -- B				
			end

		-- NCs Proveedores
		when 
			dt.docbasetype = 'APC' and i.c_letra_comprobante_id in(1010039, 1010040) then
			case	
				when i.c_letra_comprobante_id = 1010039 then '03' -- A
				else '08' -- B				
			end

		-- Para el resto de los casos no modifica el valor	
		else afipdoctype 
		end 
from c_doctype dt 
where
	i.ad_client_id = 1010016
	and i.afipdoctype is null
	and i.c_doctype_id = dt.c_doctype_id
	and (dt.docsubtypeinv in ('SF') or dt.isfiscaldocument = 'Y')
	and i.docstatus = 'CO'
;
rollback;
```

