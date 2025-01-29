DROP TABLE kategoriavklaring;

UPDATE steg_historikk
    SET steg = 'DIGITALISER_DOKUMENT'
WHERE steg = 'KATEGORISER_DOKUMENT' and aktiv = true;

UPDATE avklaringsbehov
    SET funnet_i_steg = 'DIGITALISER_DOKUMENT'
WHERE funnet_i_steg = 'KATEGORISER_DOKUMENT';
