#!/usr/bin/env python3
"""
Adi NebriEd Water Supply Authority (ዓዲ ነብሪ ኢድ ማይ ቀረብን ክፍሊትን)
Production Landscape Cashier Register & Shift Reconciliation Excel Exporter

Generates a fully styled, landscape-oriented Microsoft Excel workbook (.xlsx)
strictly adhering to the 18-column specification with dynamic formulas,
cashier shift reconciliation summary, and triple audit handover signature blocks.
"""

import sys
import os
from datetime import datetime

try:
    import openpyxl
    from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
    from openpyxl.utils import get_column_letter
except ImportError:
    print("openpyxl is not installed. To run this companion script: pip install openpyxl")
    sys.exit(0)


def generate_landscape_excel(output_filename="AdiNebriEd_Cashier_Register_Landscape.xlsx", readings=None):
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Cashier Daily Register"

    # 1. Page Setup: Landscape A4, Fit to 1 Page Wide, Repeat Rows 1:6
    ws.page_setup.orientation = ws.ORIENTATION_LANDSCAPE
    ws.page_setup.paperSize = ws.PAPERSIZE_A4
    ws.page_setup.fitToWidth = 1
    ws.page_setup.fitToHeight = 0
    ws.sheet_properties.pageSetUpProperties.fitToPage = True
    ws.print_title_rows = '1:6'  # Repeats header on every page
    ws.sheet_view.showGridLines = True

    # Palettes
    navy_dark = "003366"
    blue_header = "006699"
    light_blue_zebra = "F0F7FB"
    white = "FFFFFF"
    gray_border = "CCCCCC"
    gold_summary = "FFF8E1"

    thin_border = Border(
        left=Side(style='thin', color=gray_border),
        right=Side(style='thin', color=gray_border),
        top=Side(style='thin', color=gray_border),
        bottom=Side(style='thin', color=gray_border)
    )
    double_bottom_border = Border(
        left=Side(style='thin', color=gray_border),
        right=Side(style='thin', color=gray_border),
        top=Side(style='thin', color=gray_border),
        bottom=Side(style='double', color="000000")
    )
    box_border = Border(
        left=Side(style='medium', color="333333"),
        right=Side(style='medium', color="333333"),
        top=Side(style='medium', color="333333"),
        bottom=Side(style='medium', color="333333")
    )

    # 2. Main Title Blocks (Rows 1 to 4)
    ws.merge_cells("A1:R1")
    ws["A1"] = "ADI NEBRIED WATER SUPPLY AND SEWERAGE AUTHORITY"
    ws["A1"].font = Font(name="Calibri", size=15, bold=True, color=white)
    ws["A1"].fill = PatternFill(start_color=navy_dark, end_color=navy_dark, fill_type="solid")
    ws["A1"].alignment = Alignment(horizontal="center", vertical="center")
    ws.row_dimensions[1].height = 24

    ws.merge_cells("A2:R2")
    ws["A2"] = "ዓዲ ነብሪ ኢድ ማይ ቀረብን ክፍሊትን • OFFICIAL CASHIER DAILY SHIFT REGISTER"
    ws["A2"].font = Font(name="Calibri", size=12, bold=True, color=white)
    ws["A2"].fill = PatternFill(start_color=blue_header, end_color=blue_header, fill_type="solid")
    ws["A2"].alignment = Alignment(horizontal="center", vertical="center")
    ws.row_dimensions[2].height = 20

    today_str = datetime.now().strftime("%Y-%m-%d %H:%M")
    ws.merge_cells("A3:D3")
    ws["A3"] = f"Station: Adi NebriEd Central Station"
    ws["A3"].font = Font(name="Calibri", size=10, bold=True)

    ws.merge_cells("E3:H3")
    ws["E3"] = f"Cashier: Almaz K. (AGT-042)"
    ws["E3"].font = Font(name="Calibri", size=10, bold=True)

    ws.merge_cells("I3:L3")
    ws["I3"] = f"Shift Date: {today_str}"
    ws["I3"].font = Font(name="Calibri", size=10, bold=True)

    ws.merge_cells("M3:R3")
    ws["M3"] = "Print Setup: Landscape A4 • Fit to 1 Page Wide"
    ws["M3"].font = Font(name="Calibri", size=10, italic=True)
    ws["M3"].alignment = Alignment(horizontal="right")

    ws.merge_cells("A4:R4")
    ws["A4"] = "Tariff Tiers: 0-5m³ @10.00 | 5-15m³ @18.50 | 15-30m³ @28.00 | >30m³ @42.00 | Fixed Service: 35.00 ETB"
    ws["A4"].font = Font(name="Calibri", size=9, italic=True, color="555555")
    ws["A4"].alignment = Alignment(horizontal="center")

    # 3. 18-Column Headers (Row 6) strictly matching the specification
    headers = [
        ("No.", 5),
        ("Cust ID", 11),
        ("Meter No.", 14),
        ("Customer Full Name", 24),
        ("Phone", 14),
        ("Zone", 18),
        ("Prev Read (m³)", 13),
        ("Curr Read (m³)", 13),
        ("Consumed (m³)", 14),
        ("Water Fee (ETB)", 14),
        ("Service Fee (ETB)", 14),
        ("Arrears (ETB)", 13),
        ("Total Due (ETB)", 14),
        ("Amount Paid (ETB)", 15),
        ("Balance (ETB)", 13),
        ("Status", 11),
        ("Receipt No.", 16),
        ("Payment Channel / Remarks", 22)
    ]

    header_row = 6
    ws.row_dimensions[header_row].height = 28
    for col_idx, (header_text, col_width) in enumerate(headers, start=1):
        col_letter = get_column_letter(col_idx)
        cell = ws.cell(row=header_row, column=col_idx, value=header_text)
        cell.font = Font(name="Calibri", size=10, bold=True, color=white)
        cell.fill = PatternFill(start_color=navy_dark, end_color=navy_dark, fill_type="solid")
        cell.alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)
        cell.border = thin_border
        ws.column_dimensions[col_letter].width = col_width

    # Sample rows if none provided
    if not readings:
        readings = [
            ("CUST-001", "WM-ANE-1001", "Hagos Gebremariam", "+251914101010", "Zone 01 - Downtown", 142.50, 154.90, 0.0, 200.0, "Cash", "ANE-260919001", "NORMAL"),
            ("CUST-002", "WM-ANE-1002", "Letebirhan Woldegebriel", "+251914202020", "Zone 01 - Downtown", 89.20, 94.20, 45.0, 130.0, "Telebirr", "ANE-260919002", "NORMAL"),
            ("CUST-003", "WM-ANE-1003", "Tewolde Teklehaimanot", "+251914303030", "Zone 02 - Upper Market", 312.00, 328.50, 0.0, 400.0, "CBE Birr", "ANE-260919003", "NORMAL"),
            ("CUST-004", "WM-ANE-1004", "Abeba Berhe", "+251914404040", "Zone 02 - Upper Market", 205.80, 210.00, 120.0, 197.0, "Cash", "ANE-260919004", "NORMAL"),
            ("CUST-005", "WM-ANE-1005", "Berhane Asgedom", "+251914505050", "Kebele 03 - Residential", 58.40, 62.00, 0.0, 71.0, "Telebirr", "ANE-260919005", "NORMAL"),
            ("CUST-006", "WM-ANE-1006", "Roman Kahsay", "+251914606060", "Kebele 03 - Residential", 118.00, 124.00, 250.0, 0.0, "Unpaid", "ANE-260919006", "LEAKAGE"),
        ]

    start_data_row = 7
    for idx, item in enumerate(readings, start=1):
        r_num = start_data_row + idx - 1
        cid, meter, name, phone, zone, prev, curr, arr, paid, pay_ch, rcpt, anom = item

        ws.cell(row=r_num, column=1, value=idx).alignment = Alignment(horizontal="center")
        ws.cell(row=r_num, column=2, value=cid).alignment = Alignment(horizontal="center")
        ws.cell(row=r_num, column=3, value=meter).alignment = Alignment(horizontal="center")
        ws.cell(row=r_num, column=4, value=name).alignment = Alignment(horizontal="left")
        ws.cell(row=r_num, column=5, value=phone).alignment = Alignment(horizontal="center")
        ws.cell(row=r_num, column=6, value=zone).alignment = Alignment(horizontal="left")
        
        c_prev = ws.cell(row=r_num, column=7, value=prev)
        c_prev.number_format = "0.00"
        c_curr = ws.cell(row=r_num, column=8, value=curr)
        c_curr.number_format = "0.00"

        # Formula: Consumed = Curr - Prev
        c_cons = ws.cell(row=r_num, column=9, value=f"=H{r_num}-G{r_num}")
        c_cons.number_format = "0.00"
        c_cons.font = Font(bold=True)

        # Dynamic Water Fee Formula matching Tigray / Ethiopian Municipal Tier brackets:
        # Tier 1 (0-5) @ 10, Tier 2 (5-15) @ 18.50, Tier 3 (15-30) @ 28, Tier 4 (>30) @ 42
        water_formula = (
            f"=IF(I{r_num}<=0, 0, "
            f"IF(I{r_num}<=5, I{r_num}*10, "
            f"IF(I{r_num}<=15, 5*10 + (I{r_num}-5)*18.5, "
            f"IF(I{r_num}<=30, 5*10 + 10*18.5 + (I{r_num}-15)*28, "
            f"5*10 + 10*18.5 + 15*28 + (I{r_num}-30)*42))))"
        )
        c_wf = ws.cell(row=r_num, column=10, value=water_formula)
        c_wf.number_format = "#,##0.00"

        c_sf = ws.cell(row=r_num, column=11, value=35.00)
        c_sf.number_format = "#,##0.00"

        c_arr = ws.cell(row=r_num, column=12, value=arr)
        c_arr.number_format = "#,##0.00"

        # Total Due = Water Fee + Service Fee + Arrears
        c_tot = ws.cell(row=r_num, column=13, value=f"=J{r_num}+K{r_num}+L{r_num}")
        c_tot.number_format = "#,##0.00"
        c_tot.font = Font(bold=True)

        c_paid = ws.cell(row=r_num, column=14, value=paid)
        c_paid.number_format = "#,##0.00"

        # Balance = Total Due - Amount Paid
        c_bal = ws.cell(row=r_num, column=15, value=f"=M{r_num}-N{r_num}")
        c_bal.number_format = "#,##0.00"
        c_bal.font = Font(bold=True)

        # Status Formula
        c_stat = ws.cell(row=r_num, column=16, value=f'=IF(O{r_num}<=0.05, "PAID", IF(N{r_num}>0, "PARTIAL", "UNPAID"))')
        c_stat.alignment = Alignment(horizontal="center")
        c_stat.font = Font(bold=True)

        ws.cell(row=r_num, column=17, value=rcpt).alignment = Alignment(horizontal="center")
        ws.cell(row=r_num, column=18, value=f"{pay_ch} [{anom}]" if anom != "NORMAL" else pay_ch)

        # Apply borders and zebra striping
        fill_color = light_blue_zebra if idx % 2 == 0 else white
        for col_idx in range(1, 19):
            cell = ws.cell(row=r_num, column=col_idx)
            cell.border = thin_border
            if cell.fill.fill_type is None:
                cell.fill = PatternFill(start_color=fill_color, end_color=fill_color, fill_type="solid")

    end_data_row = start_data_row + len(readings) - 1

    # Total Sum Row
    tot_row = end_data_row + 1
    ws.merge_cells(f"A{tot_row}:H{tot_row}")
    ws[f"A{tot_row}"] = "TOTAL SHIFT ACCUMULATIONS:"
    ws[f"A{tot_row}"].font = Font(bold=True, size=11)
    ws[f"A{tot_row}"].alignment = Alignment(horizontal="right")

    for col_l in ["I", "J", "K", "L", "M", "N", "O"]:
        c = ws[f"{col_l}{tot_row}"]
        c.value = f"=SUM({col_l}{start_data_row}:{col_l}{end_data_row})"
        c.font = Font(bold=True, size=11)
        c.number_format = "#,##0.00"
        c.fill = PatternFill(start_color=gold_summary, end_color=gold_summary, fill_type="solid")

    for col_idx in range(1, 19):
        ws.cell(row=tot_row, column=col_idx).border = double_bottom_border

    # 4. Triple Signature Handover Blocks
    sig_row_start = tot_row + 3
    ws.merge_cells(f"A{sig_row_start}:F{sig_row_start}")
    ws[f"A{sig_row_start}"] = "BOX 1: CASHIER SHIFT HANDOVER"
    ws[f"A{sig_row_start}"].font = Font(bold=True, color=white)
    ws[f"A{sig_row_start}"].fill = PatternFill(start_color=navy_dark, end_color=navy_dark, fill_type="solid")

    ws.merge_cells(f"G{sig_row_start}:L{sig_row_start}")
    ws[f"G{sig_row_start}"] = "BOX 2: SENIOR REVENUE ACCOUNTANT AUDIT"
    ws[f"G{sig_row_start}"].font = Font(bold=True, color=white)
    ws[f"G{sig_row_start}"].fill = PatternFill(start_color=blue_header, end_color=blue_header, fill_type="solid")

    ws.merge_cells(f"M{sig_row_start}:R{sig_row_start}")
    ws[f"M{sig_row_start}"] = "BOX 3: STATION MANAGER FINAL APPROVAL"
    ws[f"M{sig_row_start}"].font = Font(bold=True, color=white)
    ws[f"M{sig_row_start}"].fill = PatternFill(start_color="2E7D32", end_color="2E7D32", fill_type="solid")

    box1_lines = [
        "Cashier Name: Almaz K. (AGT-042)",
        "Physical Cash Collected: =SUMIF(R7:R20, \"*Cash*\", N7:N20) ETB",
        "Digital (CBE/Telebirr): =SUM(N7:N20)-SUMIF(R7:R20, \"*Cash*\", N7:N20) ETB",
        "Signature: __________________________",
        f"Handover Timestamp: {today_str}"
    ]
    box2_lines = [
        "Auditor Name: _______________________",
        "Bank Deposit Slip Ref: _____________",
        "Physical Cash Check: [ ] VERIFIED",
        "Variance / Discrepancy: 0.00 ETB",
        "Auditor Signature: __________________"
    ]
    box3_lines = [
        "Station Branch: Adi NebriEd Central",
        "Status: VERIFIED & AUDIT APPROVED",
        "Manager Signature: __________________",
        "Official Seal: [   SEAL STAMP HERE   ]",
        f"Date Approved: {today_str[:10]}"
    ]

    for offset in range(5):
        r = sig_row_start + 1 + offset
        ws.row_dimensions[r].height = 20
        # Box 1
        ws.merge_cells(f"A{r}:F{r}")
        ws[f"A{r}"] = box1_lines[offset]
        ws[f"A{r}"].font = Font(size=10)
        # Box 2
        ws.merge_cells(f"G{r}:L{r}")
        ws[f"G{r}"] = box2_lines[offset]
        ws[f"G{r}"].font = Font(size=10)
        # Box 3
        ws.merge_cells(f"M{r}:R{r}")
        ws[f"M{r}"] = box3_lines[offset]
        ws[f"M{r}"].font = Font(size=10)

    wb.save(output_filename)
    print(f"Successfully exported Landscape Register: {output_filename}")


if __name__ == "__main__":
    generate_landscape_excel()
